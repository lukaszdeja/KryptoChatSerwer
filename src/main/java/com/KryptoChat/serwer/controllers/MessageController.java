package com.KryptoChat.serwer.controllers;

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

    public MessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("{id}")
    public ResponseEntity<MessageList> loadMessages(@PathVariable Long id) {
        MessageList messages = chatService.loadMessages(id);
        return ResponseEntity.ok(messages);
    }
}
