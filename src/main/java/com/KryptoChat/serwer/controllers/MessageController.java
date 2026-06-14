package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.DTO.MessageList;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.ChatService;
import com.KryptoChat.serwer.entities.Message;
import java.util.List;
import java.util.ArrayList;

/**
 * Kontroler odpowiedzialny za żądanie REST zaczytania wiadomości z bazy danych
 * Realizuje zaczytywanie wiadomości po uruchomieniu aplikacji (z bazy danych)
 */
@RestController
@RequestMapping("api/messages")
public class MessageController {
    private final ChatService chatService;
    private final UserService userService;
    private final JWTService jwtService;

    /**
     * Konstruktor inicjujący pola klasy
     * @param userService
     * @param chatService
     */
    public MessageController(UserService userService, ChatService chatService, JWTService jwtService) {
        this.userService = userService;
        this.chatService = chatService;
        this.jwtService = jwtService;
    }

    /**
     * Metoda obsługująca żądanie REST zaczytania wiadomości
     * Waliduje token, na jego podstawie sprawdza zalogowanego użytkownika, pobiera jego id grupy
     * Na podstawie id grupy pobiera z bazy danych wiadomości z tej grupy i zwraca ich listę opakowaną klasą MessageList
     * @param header
     * @return ResponseEntity
     */
    @GetMapping("/")
    public ResponseEntity<MessageList> loadMessages(@RequestHeader("Authorization")  String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);


        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);

        Long groupId = user.getGroup().getId();
        if (groupId == null) {
            return ResponseEntity.status(401).build();
        }
        MessageList messages = chatService.loadMessages(groupId);
        return ResponseEntity.ok(messages);
    }
}
