package com.KryptoChat.serwer.config;

import com.KryptoChat.serwer.handler.WebSocketHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class WebSocketConfigTest {
    /**
     * Metoda testująca sprawdzająca czy udało się zarejestrować mocka WebSocketHandlera pod adresem /ws
     */
    @Test
    void shouldRegisterHandlerAtWsPath() {
        WebSocketHandler mockHandler = mock(WebSocketHandler.class);
        WebSocketHandlerRegistry mockRegistry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration mockRegistration = mock(WebSocketHandlerRegistration.class);
        when(mockRegistry.addHandler(mockHandler, "/ws")).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOrigins("*")).thenReturn(mockRegistration);
        WebSocketConfig config = new WebSocketConfig(mockHandler);
        assertDoesNotThrow(() -> config.registerWebSocketHandlers(mockRegistry));
        verify(mockRegistry).addHandler(mockHandler, "/ws");
        verify(mockRegistration).setAllowedOrigins("*");
    }
}
