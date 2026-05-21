package com.KryptoChat.serwer.handler;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.UserRepository;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.KryptoChat.serwer.entities.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Map<Long, Set<WebSocketSession>> chats = new ConcurrentHashMap<>();

    private final MessageService messageService;
    private final UserRepository userRepository;

    public WebSocketHandler(UserRepository userRepository, MessageService messageService) {
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            List<String> authHeaders = session.getHandshakeHeaders().get("Authorization");

            if (authHeaders == null || authHeaders.isEmpty()) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String auth = authHeaders.getFirst();

            if (!auth.startsWith("Bearer ")) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String token = auth.substring(7);

            JWTService jwtService = new JWTService();

            if (!jwtService.isTokenValid(token)) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            Long userId = jwtService.extractUserId(token);

            session.getAttributes().put("userId", userId);

            System.out.println("User connected: " + userId);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        Message msg = mapper.readValue(message.getPayload(), Message.class);

        Long userIdLong = (Long) session.getAttributes().get("userId");

        String username = userRepository.findById(userIdLong)
                .map(User::getUsername)
                .orElse("unknown");
        msg.setSender(username);
        msg.setSend_time(LocalDateTime.now());

        Message saved = messageService.save(msg);

        Long groupId = saved.getGroupId();
        chats.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        String json = mapper.writeValueAsString(saved);

        for (WebSocketSession s : chats.get(groupId)) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        chats.values().forEach(set -> set.remove(session));

        System.out.println("User disconnected");
    }
}
