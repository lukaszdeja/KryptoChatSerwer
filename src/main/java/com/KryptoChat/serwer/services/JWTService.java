package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;


/**
 * Serwis odpowiedzialny za obsługę tokenów JWT w systemie.
 * Umożliwia generowanie tokenów dla użytkowników oraz ich walidację
 * i ekstrakcję zawartych w nich danych (userId, username, groupId).
 */
@Service
public class JWTService {


    private final String secret;
    private final Key key;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60;


    /**
     * Konstruktor inicjalizujący klucz podpisu JWT na podstawie zmiennej środowiskowej.
     * Weryfikuje poprawność długości sekretu i tworzy klucz HMAC.
     *
     * @throws IllegalStateException jeśli klucz JWT jest nieobecny lub zbyt krótki
     */
    public JWTService() {
        this.secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Klucz jest za krótki albo nie istnieje"
            );
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generuje token JWT dla podanego użytkownika.
     * Token zawiera identyfikator użytkownika, nazwę użytkownika oraz ID grupy (jeśli istnieje),
     * a także czas wydania i wygaśnięcia.
     *
     * @param user użytkownik, dla którego generowany jest token
     * @return wygenerowany token JWT
     */
    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("groupId", user.getGroup() != null ? user.getGroup().getId() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    /**
     * Sprawdza poprawność i ważność tokenu JWT.
     *
     * @param token token JWT do weryfikacji
     * @return true jeśli token jest poprawny, false jeśli wygasł lub jest niepoprawny
     */
    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Ekstrahuje identyfikator użytkownika z tokenu JWT.
     *
     * @param token token JWT
     * @return identyfikator użytkownika
     */
    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }


    /**
     * Ekstrahuje nazwę użytkownika z tokenu JWT.
     *
     * @param token token JWT
     * @return nazwa użytkownika
     */
    public String extractUsername(String token) {
        return parse(token).get("username", String.class);
    }

    /**
     * Ekstrahuje identyfikator grupy z tokenu JWT.
     *
     * @param token token JWT
     * @return identyfikator grupy lub null jeśli nie istnieje
     */
    public Long extractGroupId(String token) {
        return parse(token).get("groupId", Long.class);
    }

    /**
     * Parsuje token JWT i zwraca jego zawartość (claims).
     * Wykorzystuje klucz HMAC do weryfikacji podpisu.
     *
     * @param token token JWT
     * @return obiekt Claims zawierający dane tokenu
     */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
