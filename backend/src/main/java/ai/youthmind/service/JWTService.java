package ai.youthmind.service;

import ai.youthmind.config.JwtConfig;
import ai.youthmind.dto.TokenPair;
import ai.youthmind.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class JWTService {

    private final JwtConfig jwtConfig;
    private final RedisService redisService;
    private final AuditLogService auditLogService;

    private final List<Consumer<String>> expirationAlertListeners = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USED_REFRESH_PREFIX = "jwt:used_refresh:";
    private static final String TOKEN_EXPIRATION_PREFIX = "jwt:token_expiration:";
    private static final String USER_TOKENS_PREFIX = "jwt:user_tokens:";

    public JWTService(JwtConfig jwtConfig, RedisService redisService, AuditLogService auditLogService) {
        this.jwtConfig = jwtConfig;
        this.redisService = redisService;
        this.auditLogService = auditLogService;
        scheduler.scheduleAtFixedRate(this::monitorTokenExpirations, 1, 1, TimeUnit.MINUTES);
    }

    public TokenPair generateTokenPair(String email) {
        Pair<String, Timestamp> accessToken = generateToken(email);
        Pair<String, Timestamp> refreshToken = generateRefreshToken(email);
        return new TokenPair(
                accessToken.getFirst(),
                accessToken.getSecond(),
                refreshToken.getFirst(),
                refreshToken.getSecond()
        );
    }

    public Pair<String, Timestamp> generateToken(String email) {
        Pair<String, Timestamp> token = generateToken(email, jwtConfig.getExpiration(), "access");

        auditLogService.logSecurityEvent("TOKEN_GENERATED", email,
                "Access token generated", true);

        return token;
    }

    public Pair<String, Timestamp> generateRefreshToken(String email) {
        Pair<String, Timestamp> refreshToken = generateToken(email, jwtConfig.getRefreshExpiration(), "refresh");

        String userTokensKey = USER_TOKENS_PREFIX + email;
        redisService.addToSet(userTokensKey, refreshToken.getFirst());
        redisService.setExpiration(userTokensKey, jwtConfig.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        auditLogService.logSecurityEvent("REFRESH_TOKEN_GENERATED", email,
                "Refresh token generated", true);

        return refreshToken;
    }

    private Pair<String, Timestamp> generateToken(String email, long expirationMs, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", tokenType);
        long currentTime = System.currentTimeMillis();
        Timestamp expiration = new Timestamp(currentTime + expirationMs);

        String tokenString = Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(currentTime))
                .expiration(expiration)
                .and()
                .signWith(getKey())
                .compact();

        String expirationKey = TOKEN_EXPIRATION_PREFIX + tokenString;
        redisService.setValue(expirationKey, expiration.getTime());
        redisService.setExpiration(expirationKey, expirationMs, TimeUnit.MILLISECONDS);

        return Pair.of(tokenString, expiration);
    }

    private SecretKey getKey() {
        byte[] bytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(bytes);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, User user) {
        final String email = extractEmail(token);
        final String type = extractTokenType(token);
        boolean isValid = (email.equals(user.getEmail()) && "access".equals(type) &&
                !isTokenExpired(token) && !isTokenBlacklisted(token));

        auditLogService.logSecurityEvent("TOKEN_VALIDATION", email,
                String.format("Token validation attempt - success: %s", isValid), isValid);

        return isValid;
    }

    public boolean validateRefreshToken(String token, User user) {
        final String email = extractEmail(token);
        final String type = extractTokenType(token);
        boolean isValid = (email.equals(user.getEmail()) && "refresh".equals(type) &&
                !isTokenExpired(token) && !isTokenBlacklisted(token) && !isRefreshTokenUsed(token));

        auditLogService.logSecurityEvent("REFRESH_TOKEN_VALIDATION", email,
                String.format("Refresh token validation attempt - success: %s", isValid), isValid);

        return isValid;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public TokenPair refreshTokens(String refreshToken, User user) {
        if (validateRefreshToken(refreshToken, user)) {
            String email = user.getEmail();
            markRefreshTokenAsUsed(refreshToken);
            TokenPair response = generateTokenPair(email);

            auditLogService.logSecurityEvent("TOKEN_REFRESHED", email,
                    "Tokens refreshed successfully", true);

            return response;
        } else {
            throw new SecurityException("Invalid refresh token");
        }
    }

    public void blacklistToken(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        long ttl = extractExpiration(token).getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisService.setValue(blacklistKey, "true");
            redisService.setExpiration(blacklistKey, ttl, TimeUnit.MILLISECONDS);
        }

        redisService.deleteValue(TOKEN_EXPIRATION_PREFIX + token);

        String email = extractEmail(token);
        auditLogService.logSecurityEvent("TOKEN_BLACKLISTED", email,
                "Token blacklisted", true);
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisService.hasKey(BLACKLIST_PREFIX + token));
    }

    public void markRefreshTokenAsUsed(String refreshToken) {
        String usedRefreshKey = USED_REFRESH_PREFIX + refreshToken;
        long ttl = extractExpiration(refreshToken).getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisService.setValue(usedRefreshKey, "true");
            redisService.setExpiration(usedRefreshKey, ttl, TimeUnit.MILLISECONDS);
        }

        String email = extractEmail(refreshToken);
        auditLogService.logSecurityEvent("REFRESH_TOKEN_USED", email,
                "Refresh token marked as used", true);

        blacklistToken(refreshToken);
    }

    public boolean isRefreshTokenUsed(String refreshToken) {
        return Boolean.TRUE.equals(redisService.hasKey(USED_REFRESH_PREFIX + refreshToken));
    }

    private void monitorTokenExpirations() {
        String pattern = TOKEN_EXPIRATION_PREFIX + "*";
        Cursor<byte[]> cursor = null;

        try {
            cursor = redisService.execute(connection ->
                    connection.keyCommands().scan(ScanOptions.scanOptions().match(pattern).count(100).build())
            );

            long currentTime = System.currentTimeMillis();
            long warningThreshold = jwtConfig.getExpirationWarningThreshold();

            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                String token = key.substring(TOKEN_EXPIRATION_PREFIX.length());

                Long expirationTime = (Long) redisService.getValue(key);
                if (expirationTime != null) {
                    long timeUntilExpiration = expirationTime - currentTime;

                    if (timeUntilExpiration > 0 && timeUntilExpiration <= warningThreshold) {
                        try {
                            String email = extractEmail(token);
                            String alertMessage = String.format(
                                    "Token for user %s is expiring in %d minutes",
                                    email, TimeUnit.MILLISECONDS.toMinutes(timeUntilExpiration)
                            );

                            for (Consumer<String> listener : expirationAlertListeners) {
                                try {
                                    listener.accept(alertMessage);
                                } catch (Exception e) {
                                    System.err.println("Error in expiration alert listener: " + e.getMessage());
                                }
                            }

                            redisService.deleteValue(key);
                        } catch (Exception e) {
                            redisService.deleteValue(key);
                        }
                    } else if (timeUntilExpiration <= 0) {
                        redisService.deleteValue(key);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error monitoring token expirations: " + e.getMessage());
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    System.err.println("Error closing cursor: " + e.getMessage());
                }
            }
        }
    }

    public void addExpirationAlertListener(Consumer<String> listener) {
        expirationAlertListeners.add(listener);
    }

    public void removeExpirationAlertListener(Consumer<String> listener) {
        expirationAlertListeners.remove(listener);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

}
