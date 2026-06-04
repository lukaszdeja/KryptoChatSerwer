package com.KryptoChat.serwer.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityConfigTest {
    /**
     * Metoda testujaca czy udalo sie utworzyc passwordEncoder do Bcrypta
     */
    @Test
    void shouldReturnBCryptPasswordEncoder() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    /**
     * Metoda testujaca sprawdzajaca czy dziala prawidlowo porownywanie hashy hasel
     */
    @Test
    void shouldEncodeAndMatchPassword() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        String raw = "tajneHaslo123";
        String encoded = encoder.encode(raw);
        assertTrue(encoder.matches(raw, encoded));
    }

    /**
     * Metoda testujaca sprawdzajaca czy rozne hasla po porownaniu daja wynik false
     */
    @Test
    void shouldNotMatchWrongPassword() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        assertFalse(encoder.matches("zleHaslo", encoder.encode("dobreHaslo")));
    }
}
