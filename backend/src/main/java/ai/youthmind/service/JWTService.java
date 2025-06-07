package ai.youthmind.service;

import ai.youthmind.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTService {

    private JwtConfig jwtConfig;

    public JWTService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

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

    private Key getKey() {
        byte[] bytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(bytes);
    }

}
