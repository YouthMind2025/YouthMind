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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class JWTService {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private RedisService redisService;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    public Pair<String, Timestamp> generateToken(String email) {

        Map<String, Object> claims = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        Timestamp expiration = new Timestamp(currentTime + jwtConfig.getExpiration());

        String tokenString = Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(currentTime))
                .expiration(expiration)
                .and()
                .signWith(getKey())
                .compact();
        return Pair.of(tokenString, expiration);
    }

    private SecretKey getKey() {
        byte[] bytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(bytes);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
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
            redisService.setValue(blacklistKey, "true");
            redisService.setExpiration(blacklistKey, ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisService.hasKey(BLACKLIST_PREFIX + token));
    }

}
