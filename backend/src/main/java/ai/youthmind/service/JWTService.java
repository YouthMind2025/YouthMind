package ai.youthmind.service;

import ai.youthmind.config.JwtConfig;
import ai.youthmind.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class JWTService {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private RedisService redisService;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_PREFIX = "jwt:user_tokens:";

    public Pair<String, Timestamp> generateToken(String email) {
        return generateToken(email, "USER");
    }

    public Pair<String, Timestamp> generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        long currentTime = System.currentTimeMillis();
        Timestamp expiration = new Timestamp(currentTime + jwtConfig.getExpiration());

        String token = Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(currentTime))
                .expiration(expiration)
                .and()
                .signWith(getKey())
                .compact();

        updateUserTokens(email, token);
        return Pair.of(token, expiration);
    }

    private SecretKey getKey() {
        byte[] bytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(bytes);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
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
        return (email.equals(user.getEmail()) && !isTokenExpired(token) && !isTokenBlacklisted(token));
    }

    public boolean validateToken(String token) {
        return !isTokenExpired(token) && !isTokenBlacklisted(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public void blacklistToken(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        long ttl = extractExpiration(token).getTime() - System.currentTimeMillis();

        if (ttl > 0) {
             redisService.setValue(blacklistKey, true);
             redisService.setExpiration(blacklistKey, ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            return redisService.hasKey(key) && (boolean) redisService.getValue(key);
        } catch (Exception e) {
            // If Redis is unavailable, assume token is not blacklisted (fail open)
            System.err.println("⚠️ Warning: Unable to check token blacklist in Redis: " + e.getMessage());
            return false;
        }
    }

    public void updateUserTokens(String email, String token) {
        try {
            String key = USER_TOKENS_PREFIX + email;
            List<String> tokens = redisService.hasKey(key)?
                    (List<String>) redisService.getValue(key) : new ArrayList<>();

            tokens.add(token);
            tokens.removeIf(this::validateToken);

            redisService.setValue(key, tokens);
        } catch (Exception e) {
            // Log warning but don't fail token generation if Redis is unavailable
            System.err.println("⚠️ Warning: Unable to update user tokens in Redis: " + e.getMessage());
        }
    }

    public void blacklistUserTokens(String email) {
        String key = USER_TOKENS_PREFIX + email;
        
        try {
            // Check if the user tokens key exists in Redis
            if (redisService.hasKey(key)) {
                System.out.println("🔍 DEBUG: Blacklisting tokens for user: " + email);
                
                // Get the list of tokens for this user
                Object tokensObj = redisService.getValue(key);
                if (tokensObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> tokens = (List<String>) tokensObj;
                    
                    System.out.println("🔍 DEBUG: Found " + tokens.size() + " tokens to blacklist for user: " + email);
                    
                    // Blacklist each individual token
                    for (String token : tokens) {
                        if (token != null && !token.trim().isEmpty()) {
                            try {
                                blacklistToken(token);
                                System.out.println("🔍 DEBUG: Successfully blacklisted token: " + token.substring(0, Math.min(20, token.length())) + "...");
                            } catch (Exception e) {
                                System.err.println("⚠️ Warning: Failed to blacklist token for user " + email + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    // Clear the user's token list after blacklisting all tokens
                    redisService.setValue(key, new ArrayList<>());
                    System.out.println("🔍 DEBUG: Cleared token list for user: " + email);
                } else {
                    System.err.println("⚠️ Warning: Invalid token list format for user: " + email);
                }
            } else {
                System.out.println("🔍 DEBUG: No tokens found for user: " + email);
            }
        } catch (Exception e) {
            // Log error but don't fail the operation if Redis is unavailable
            System.err.println("⚠️ Warning: Unable to blacklist tokens for user " + email + ": " + e.getMessage());
        }
    }

}
