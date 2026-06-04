package com.KryptoChat.serwer.handler;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import com.KryptoChat.serwer.repositories.UserRepository;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final Map<Long, WebSocketSession> activeUsers = new ConcurrentHashMap<>();
    private final JWTService jwtService;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final GroupKeyRepository groupKeyRepository;

    public WebSocketHandler(UserRepository userRepository, MessageService messageService, JWTService jwtService, GroupKeyRepository gkr) {
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.jwtService = jwtService;
        this.groupKeyRepository = gkr;
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


            if (!jwtService.isTokenValid(token)) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            Long userId = jwtService.extractUserId(token);

            session.getAttributes().put("userId", userId);
            activeUsers.put(userId, session);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getGroup() != null) {
                Long groupId = user.getGroup().getId();
                session.getAttributes().put("groupId", groupId);
                chats.computeIfAbsent(user.getGroup().getId(), k -> ConcurrentHashMap.newKeySet()).add(session);
            }
            notifyPendingMembers(userId, session);

            System.out.println("User connected: " + userId);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void notifyPendingMembers(Long userId, WebSocketSession session) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getGroup() == null)  {
                return;
            }
            Long groupId = user.getGroup().getId();
            GroupKey myKey = groupKeyRepository.findByGroupIdAndUserId(groupId, userId).orElse(null);
            if (myKey == null) return;

            if ("ACTIVE".equals(myKey.getStatus())) {

                List<GroupKey> pending = groupKeyRepository.findByGroupIdAndStatus(groupId, "PENDING");

                for (GroupKey pendingKey : pending) {
                    User pendingUser = userRepository.findById(pendingKey.getUserId()).orElse(null);
                    if (pendingUser == null) continue;

                    Map<String, Object> msg = Map.of(
                            "type",      "KEY_REQUEST",
                            "userId",    pendingUser.getId(),
                            "username",  pendingUser.getUsername(),
                            "publicKey", pendingUser.getPublicKey()
                    );
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
                }

            } else if ("PENDING".equals(myKey.getStatus())) {
                Map<String, Object> msg = Map.of(
                        "type",      "KEY_REQUEST",
                        "userId",    user.getId(),
                        "username",  user.getUsername(),
                        "publicKey", user.getPublicKey()
                );

                List<GroupKey> activeKeys = groupKeyRepository.findByGroupIdAndStatus(groupId, "ACTIVE");

                for (GroupKey activeKey : activeKeys) {
                    WebSocketSession activeSession = activeUsers.get(activeKey.getUserId());
                    if (activeSession != null && activeSession.isOpen()) {
                        activeSession.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifyKeyReady(Long userId) {
        System.out.println("notifyKeaReady lda usera" + userId);
        WebSocketSession session = activeUsers.get(userId);
        if (session == null) {
            System.out.println("Brak sesji");
            return;
        }

        if(!session.isOpen()) {
            System.out.println("Sesja zamknieta");
            return;
        }
        try {
            Map<String, String> msg = Map.of("type", "KEY_READY");
            String json = mapper.writeValueAsString(msg);
            System.out.println("Wysylam json: " + json);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        JsonNode node = mapper.readTree(message.getPayload());
        String type = node.has("type") ? node.get("type").asText() : "CHAT";
        if ("CHAT".equals(type)) {
            node = node.deepCopy();
            ((ObjectNode) node).remove("type");
            Message msg = mapper.treeToValue(node, Message.class);
            Long userIdLong = (Long) session.getAttributes().get("userId");
            Long groupId = (Long) session.getAttributes().get("groupId");
            User user = userRepository.findById(userIdLong).orElseThrow(() -> new RuntimeException("User not found"));;
            if (user == null) {
                return;
            }
            String content = node.has("content") ? node.get("content").asText() : null;
            if (content == null || content == "" || content.length() > 500) {
                return;
            }
            if (user.getGroup().getId() == null || !user.getGroup().getId().equals(groupId)) {
                return;
            }
            String username = user.getUsername();
            msg.setSender(username);
            msg.setSend_time(LocalDateTime.now());
            msg.setGroupId(groupId);
            msg.setContent(content);
            Message saved = messageService.save(msg);
            chats.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(session);
            String json = mapper.writeValueAsString(saved);
            for (WebSocketSession s : chats.get(groupId)) {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        chats.values().forEach(set -> set.remove(session));
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            activeUsers.remove(userId);
        }
        System.out.println("User disconnected");
    }
}
