package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.ChatService;
import com.KryptoChat.serwer.entities.Message;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("api/messages")
public class MessageController {
    private final ChatService chatService;
    private final UserService userService;

    public MessageController(UserService userService, ChatService chatService) {
        this.userService = userService;
        this.chatService = chatService;
    }

    @GetMapping("/")
    public ResponseEntity<MessageList> loadMessages(@RequestHeader("Authorization")  String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);

        JWTService jwtService = new JWTService();

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);

        Long groupId = user.getGroup().getId();
        MessageList messages = chatService.loadMessages(groupId);
        return ResponseEntity.ok(messages);
    }
}
