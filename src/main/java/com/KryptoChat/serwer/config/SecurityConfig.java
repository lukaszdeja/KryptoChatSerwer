package com.KryptoChat.serwer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Klasa pomocnicza odpowiedzialna za hashowanie haseł użytkowników w bazie danych
 * Oraz porównywanie przesłanych przez użytkowników haseł poprzez REST API z zapisanymi hashami
 * A także za bezpieczeństwo połączeń https
 */
@Configuration
public class SecurityConfig {

    /**
     * Metoda pomocnicza zwracająca obiekt BCryptEncodera
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Metoda odpowiedzialna z filtrowanie przychodzących połączeń, przepuszcza wszystkie połączenia http
     * Jest konieczna aby BCryptPasswordEncoder działał
     * @param http
     * @return SecurityFilterChain - filtr
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
