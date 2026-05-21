package com.KryptoChat.serwer.config;

import com.KryptoChat.serwer.handler.WebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * Klasa konfigurująca działające WebSocketHandlery
 * Określa ścieżkę po jakiej klienci mogą łączyć się z serwerem gniazdem sieciowym
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler handler;

    /**
     * Konstruktor klasy,, inicjuje WebSocketHandler
     * @param handler - WebSocketHandler
     */
    public WebSocketConfig(WebSocketHandler handler) {
        this.handler = handler;
    }

    /**
     * Metoda, która rejestruje WebSocketHandler, dodaje go do ścieżki /ws
     * @param registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(handler, "/ws")
                .setAllowedOrigins("*");
    }
}