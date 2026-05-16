package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JWTService {


    private final String secret;
    private final Key key;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60;


    public JWTService() {
        this.secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Klucz jest za krótki albo nie istnieje"
            );
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }


    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("groupId", user.getGroup().getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }


    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }


    public String extractUsername(String token) {
        return parse(token).get("username", String.class);
    }


    public Long extractGroupId(String token) {
        return parse(token).get("groupId", Long.class);
    }


    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
