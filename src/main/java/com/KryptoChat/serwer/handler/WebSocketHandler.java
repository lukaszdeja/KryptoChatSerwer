package com.KryptoChat.serwer.handler;
import com.KryptoChat.serwer.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.KryptoChat.serwer.entities.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Map<Long, Set<WebSocketSession>> chats = new ConcurrentHashMap<>();

    private final MessageService messageService;

    public WebSocketHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            String query = uri.getQuery();
            String userId = query.split("=")[1];
            session.getAttributes().put("userId", userId);
            System.out.println("User connected: " + userId);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        Message msg = mapper.readValue(message.getPayload(), Message.class);

        String userId = (String) session.getAttributes().get("userId");

        msg.setSender(userId);
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
