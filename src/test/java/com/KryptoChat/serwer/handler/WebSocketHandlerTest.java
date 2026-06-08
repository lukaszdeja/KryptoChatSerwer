package com.KryptoChat.serwer.handler;

import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.Message;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import com.KryptoChat.serwer.repositories.UserRepository;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//Konieczny tryb LENIENT
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketHandlerTest {
    /**
     * Wykorzystywane w testach mocki
     */
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private JWTService jwtService;
    @Mock
    private GroupKeyRepository groupKeyRepository;

    @InjectMocks
    private WebSocketHandler handler;

    private User user;
    private Group group;

    /**
     * Setup testowanych obiektow
     */
    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(10L);
        group.setGroupName("testGroup");

        user = new User();
        user.setId(1L);
        user.setUsername("lukasz");
        user.setGroup(group);
        user.setPublicKey("public-key-123");
        lenient().when(groupKeyRepository.findByGroupIdAndStatus(anyLong(), anyString()))
                .thenReturn(List.of());
    }

    /**
     * Metoda pomocnicza tworzoca mockowana sesje
     * @return WebSocketSession
     */
    private WebSocketSession mockSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        return session;
    }

    /**
     * Metoda pomocnicza do autoryzowanych headerow
     * @param value
     * @return
     */
    private HttpHeaders authHeader(String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", value);
        return headers;
    }

    /**
     * Metoda pomocnicza tworzoca token jwt
     * @param session
     * @param userId
     * @param token
     */
    private void setupJwt(WebSocketSession session, Long userId, String token) {
        HttpHeaders headers = authHeader("Bearer " + token);
        when(session.getHandshakeHeaders()).thenReturn(headers);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
    }

    /**
     * Metoda pomocnicza tworzaca klucz grupy (obiekt)
     * @param groupId
     * @param userId
     * @param encKey
     * @param status
     * @return
     */
    private GroupKey buildKey(Long groupId, Long userId, String encKey, String status) {
        GroupKey key = new GroupKey();
        key.setGroupId(groupId);
        key.setUserId(userId);
        key.setEncryptedGroupKey(encKey);
        key.setStatus(status);
        return key;
    }

    /**
     * Metoda pomocnicza tworzaca uzytkownika
     * @param id
     * @param username
     * @param publicKey
     * @return
     */
    private User buildUser(Long id, String username, String publicKey) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPublicKey(publicKey);
        return u;
    }

    /**
    * Metoda pomocnicza wiazaca usera ze statusem klucza i zwracajaca sesje websocketowa
     */
    private WebSocketSession connectUser(User u, String keyStatus) throws Exception {
        WebSocketSession session = mockSession();
        setupJwt(session, u.getId(), "token-" + u.getId());
        when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        Long groupId = u.getGroup() != null ? u.getGroup().getId() : null;
        if (groupId != null) {
            GroupKey myKey = buildKey(groupId, u.getId(), "enc-key", keyStatus);
            when(groupKeyRepository.findByGroupIdAndUserId(groupId, u.getId()))
                    .thenReturn(Optional.of(myKey));
        }

        handler.afterConnectionEstablished(session);
        return session;
    }

    /**
     * Metoda sprawdzajaca czy jezeli nie ma headera (websocket) to czy odrzuci polaczenie
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldCloseWhenNoAuthHeader() throws Exception {
        WebSocketSession session = mockSession();
        when(session.getHandshakeHeaders()).thenReturn(new HttpHeaders());
        handler.afterConnectionEstablished(session);
        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
    }

    /**
     * Metoda pomocnicza sprawdzajaca czy jezeli header ma zla tresc to czy polaczenie bedzie odrzucone
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldCloseWhenMissingBearerPrefix() throws Exception {
        WebSocketSession session = mockSession();
        when(session.getHandshakeHeaders()).thenReturn(authHeader("Basic some-token"));
        handler.afterConnectionEstablished(session);
        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
    }

    /**
     * Metoda sprawdzajaca czy polaczenia z nieprawidlowym tokenem beda odrzucane
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldCloseWhenTokenIsInvalid() throws Exception {
        WebSocketSession session = mockSession();
        when(session.getHandshakeHeaders()).thenReturn(authHeader("Bearer bad-token"));
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);
        handler.afterConnectionEstablished(session);
        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
    }

    /**
     * Metoda sprawdzajaca czy po prawidlowym urzadzeniu sesja usera jest prawidlowo zapisana
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldStoreUserIdInSessionAttributes() throws Exception {
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING")).thenReturn(List.of());
        WebSocketSession session = connectUser(user, "ACTIVE");
        assertEquals(1L, session.getAttributes().get("userId"));
    }

    /**
     * Test sprawdzajacy czy id grupy jest zapisywane (sesja)
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldStoreGroupIdWhenUserHasGroup() throws Exception {
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING")).thenReturn(List.of());
        WebSocketSession session = connectUser(user, "ACTIVE");
        assertEquals(10L, session.getAttributes().get("groupId"));
    }

    /**
     * Test sprawdzajacy czy user ktory nie jest w grupie (taka sytuacja domyslnie nigdy nie wystapi,
     * ale zabezpieczenie przeciw atakom) po polaczeniu nie zapisuje sesji jakiegos id grupy
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldNotSetGroupIdWhenUserHasNoGroup() throws Exception {
        user.setGroup(null);
        WebSocketSession session = mockSession();
        setupJwt(session, 1L, "token-1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        handler.afterConnectionEstablished(session);
        assertNull(session.getAttributes().get("groupId"));
    }

    /**
     * Dla dobrego tokenu nie odrzucamy sesji
     * @throws Exception
     */
    @Test
    void afterConnectionEstablished_shouldNotCloseSessionOnValidToken() throws Exception {
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING")).thenReturn(List.of());
        WebSocketSession session = connectUser(user, "ACTIVE");
        verify(session, never()).close(any(CloseStatus.class));
    }

    /**
     * Test sprawdzajacy czy jest klucz jest wysylany do pending
     * @throws Exception
     */
    @Test
    void notifyPendingMembers_activeUser_shouldSendKeyRequestForEachPendingMember() throws Exception {
        User pendingUser = buildUser(2L, "pending-user", "pending-pub-key");
        GroupKey pendingKey = buildKey(10L, 2L, null, "PENDING");
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING"))
                .thenReturn(List.of(pendingKey));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        WebSocketSession session = connectUser(user, "ACTIVE");
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        String sentJson = captor.getAllValues().stream()
                .map(TextMessage::getPayload)
                .filter(p -> p.contains("KEY_REQUEST"))
                .findFirst()
                .orElse("");
        assertFalse(sentJson.isEmpty(), "Powinna zostać wysłana wiadomość KEY_REQUEST");
        assertTrue(sentJson.contains("pending-pub-key"), "JSON powinien zawierać klucz publiczny pending usera");
        assertTrue(sentJson.contains("pending-user"), "JSON powinien zawierać username pending usera");
    }

    /**
     * Test sprawdzajacy czy jesli nie ma pending memberow to czy cos jest wysylane
     * @throws Exception
     */
    @Test
    void notifyPendingMembers_activeUser_shouldNotSendAnythingWhenNoPendingMembers() throws Exception {
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING")).thenReturn(List.of());
        WebSocketSession session = connectUser(user, "ACTIVE");
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    /**
     * Test sprawdzajacy czy klucz jest wysylany do wszystkich pending w grupie a nie tylko jednej osoby
     * @throws Exception
     */
    @Test
    void notifyPendingMembers_activeUser_shouldSendKeyRequestForMultiplePendingMembers() throws Exception {
        User pending1 = buildUser(2L, "pending-one", "pub-key-one");
        User pending2 = buildUser(3L, "pending-two", "pub-key-two");
        GroupKey key1 = buildKey(10L, 2L, null, "PENDING");
        GroupKey key2 = buildKey(10L, 3L, null, "PENDING");
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING"))
                .thenReturn(List.of(key1, key2));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pending1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(pending2));
        WebSocketSession session = connectUser(user, "ACTIVE");
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        long keyRequestCount = captor.getAllValues().stream()
                .filter(m -> m.getPayload().contains("KEY_REQUEST"))
                .count();
        assertEquals(2, keyRequestCount, "Powinny zostać wysłane 2 wiadomości KEY_REQUEST");
    }

    /**
     * Test sprawdzajacy czy podlaczeni uzytkownicy ze statusem active dostaja zapytanie o klucz
     * @throws Exception
     */
    @Test
    void notifyPendingMembers_pendingUser_shouldNotifyOnlineActiveMembers() throws Exception {
        User activeUser = buildUser(2L, "admin", "admin-pub-key");
        activeUser.setGroup(group);
        WebSocketSession activeSession = mockSession();
        setupJwt(activeSession, 2L, "active-token");
        when(userRepository.findById(2L)).thenReturn(Optional.of(activeUser));
        GroupKey activeKey = buildKey(10L, 2L, "enc-key", "ACTIVE");
        when(groupKeyRepository.findByGroupIdAndUserId(10L, 2L)).thenReturn(Optional.of(activeKey));
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "PENDING")).thenReturn(List.of());
        handler.afterConnectionEstablished(activeSession);
        WebSocketSession pendingSession = mockSession();
        setupJwt(pendingSession, 1L, "pending-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        GroupKey pendingKey = buildKey(10L, 1L, null, "PENDING");
        when(groupKeyRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(pendingKey));
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "ACTIVE")).thenReturn(List.of(activeKey));
        when(activeSession.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(pendingSession);
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(activeSession, atLeastOnce()).sendMessage(captor.capture());
        String sentJson = captor.getAllValues().stream()
                .map(TextMessage::getPayload)
                .filter(p -> p.contains("KEY_REQUEST"))
                .findFirst()
                .orElse("");
        assertFalse(sentJson.isEmpty(), "Aktywny użytkownik powinien otrzymać KEY_REQUEST");
        assertTrue(sentJson.contains(user.getPublicKey()), "JSON powinien zawierać klucz publiczny pending usera");
        assertTrue(sentJson.contains(user.getUsername()), "JSON powinien zawierać username pending usera");
    }

    /**
     * Test sprawdzajacy czy jesli nikt nie jest podlaczony to czy cos jest wysylane
     * @throws Exception
     */
    @Test
    void notifyPendingMembers_pendingUser_shouldNotSendWhenActiveMemberIsOffline() throws Exception {
        WebSocketSession session = mockSession();
        setupJwt(session, 1L, "token-1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupKeyRepository.findByGroupIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(buildKey(10L, 1L, null, "PENDING")));
        when(groupKeyRepository.findByGroupIdAndStatus(10L, "ACTIVE"))
                .thenReturn(List.of(buildKey(10L, 99L, "key", "ACTIVE")));
        handler.afterConnectionEstablished(session);
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    /**
     * Test sprawdzajacy jak zadziala metoda jesli ktos nie ma klucza
     * @throws Exception
     */
    @Test
    void notifyPendingMembers_shouldDoNothingWhenUserHasNoGroupKey() throws Exception {
        WebSocketSession session = mockSession();
        setupJwt(session, 1L, "token-1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupKeyRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        handler.afterConnectionEstablished(session);
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    /**
     * Test wyslania wiadomosci o tym ze klucz jest gotowy
     * @throws Exception
     */
    @Test
    void notifyKeyReady_shouldSendKeyReadyMessageToConnectedUser() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(true);
        handler.notifyKeyReady(1L);
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        boolean found = captor.getAllValues().stream()
                .anyMatch(m -> m.getPayload().contains("KEY_READY"));
        assertTrue(found, "Powinna zostać wysłana wiadomość KEY_READY");
    }

    /**
     * Test co robi metoda jak user ktory ma dostac klucz nie jest polaczony
     */
    @Test
    void notifyKeyReady_shouldDoNothingWhenUserIsNotConnected() {
        assertDoesNotThrow(() -> handler.notifyKeyReady(999L));
        verifyNoInteractions(messageService);
    }

    /**
     * Test co robi metoda jak sesja zostala zamknieta
     * @throws Exception
     */
    @Test
    void notifyKeyReady_shouldDoNothingWhenSessionIsClosed() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(false);
        handler.notifyKeyReady(1L);
        verify(session, never()).sendMessage(argThat(
                msg -> msg instanceof TextMessage &&
                        ((TextMessage) msg).getPayload().contains("KEY_READY")
        ));
    }

    /**
     * Test czy zweryfikowane wiadomosci zostaja zapisane w bazie danych
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldSaveAndBroadcastValidChatMessage() throws Exception {
        // Przygotuj sesję z zalogowanym użytkownikiem
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(true);
        Message savedMessage = new Message();
        savedMessage.setId(42L);
        savedMessage.setSender("lukasz");
        savedMessage.setContent("Cześć!");
        savedMessage.setGroupId(10L);
        when(messageService.save(any(Message.class))).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String payload = "{\"type\":\"CHAT\",\"content\":\"Cześć!\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(messageService).save(any(Message.class));
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        boolean broadcastFound = captor.getAllValues().stream()
                .anyMatch(m -> m.getPayload().contains("Cześć!"));
        assertTrue(broadcastFound, "Wiadomość powinna zostać rozgłoszona do grupy");
    }

    /**
     * Test czy zweryfikowane wiadomosci są broadcastowane do innych czlonkow grupy
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldBroadcastToAllSessionsInGroup() throws Exception {
        WebSocketSession session1 = connectUser(user, "ACTIVE");
        when(session1.isOpen()).thenReturn(true);
        User user2 = buildUser(2L, "anna", "pub-key-anna");
        user2.setGroup(group);
        WebSocketSession session2 = connectUser(user2, "ACTIVE");
        when(session2.isOpen()).thenReturn(true);
        Message savedMessage = new Message();
        savedMessage.setSender("lukasz");
        savedMessage.setContent("Hej wszystkim!");
        savedMessage.setGroupId(10L);
        when(messageService.save(any(Message.class))).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String payload = "{\"type\":\"CHAT\",\"content\":\"Hej wszystkim!\"}";
        handler.handleTextMessage(session1, new TextMessage(payload));
        ArgumentCaptor<TextMessage> captor1 = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1, atLeastOnce()).sendMessage(captor1.capture());
        assertTrue(captor1.getAllValues().stream()
                .anyMatch(m -> m.getPayload().contains("Hej wszystkim!")));
        ArgumentCaptor<TextMessage> captor2 = ArgumentCaptor.forClass(TextMessage.class);
        verify(session2, atLeastOnce()).sendMessage(captor2.capture());
        assertTrue(captor2.getAllValues().stream()
                .anyMatch(m -> m.getPayload().contains("Hej wszystkim!")));
    }

    /**
     * Test czy puste wiadomosci sa zapisywane (zabezpieczenie przed tym)
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldNotSaveWhenContentIsEmpty() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String payload = "{\"type\":\"CHAT\",\"content\":\"\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(messageService, never()).save(any(Message.class));
    }

    /**
     * Test czy za dlugie wiadomosci nie sa odrzucane (zabezpieczenie bazy)
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldNotSaveWhenContentExceeds500Characters() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String longContent = "a".repeat(1501);
        String payload = "{\"type\":\"CHAT\",\"content\":\"" + longContent + "\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(messageService, never()).save(any(Message.class));
    }

    /**
     * Test czy wrazliwe dane takie jak login sa brane z backendu a nie z requesta (z bazy sa brane)
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldSetSenderUsernameFromRepository() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(true);
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        Message savedMessage = new Message();
        savedMessage.setSender("lukasz");
        savedMessage.setContent("Test");
        savedMessage.setGroupId(10L);
        when(messageService.save(msgCaptor.capture())).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String payload = "{\"type\":\"CHAT\",\"content\":\"Test\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        assertEquals("lukasz", msgCaptor.getValue().getSender(),
                "Nazwa nadawcy powinna być pobrana z repozytorium użytkowników");
    }

    /**
     * Test czy id grupy jest brane z danych sesji, czy ktos nie atakuje, nieautoryzowany dostep odrzuca
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldSetGroupIdFromSessionAttributes() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(true);
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        Message savedMessage = new Message();
        savedMessage.setSender("lukasz");
        savedMessage.setContent("Test");
        savedMessage.setGroupId(10L);
        when(messageService.save(msgCaptor.capture())).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String payload = "{\"type\":\"CHAT\",\"content\":\"Test\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        assertEquals(10L, msgCaptor.getValue().getGroupId(),
                "GroupId wiadomości powinien być pobrany z atrybutów sesji");
    }

    /**
     * Test czy wysyla na zamkniete sesje
     * @throws Exception
     */
    @Test
    void handleTextMessage_shouldNotBroadcastToClosedSessions() throws Exception {
        WebSocketSession session1 = connectUser(user, "ACTIVE");
        when(session1.isOpen()).thenReturn(true);
        User user2 = buildUser(2L, "anna", "pub-key-anna");
        user2.setGroup(group);
        WebSocketSession session2 = connectUser(user2, "ACTIVE");
        when(session2.isOpen()).thenReturn(false);
        Message savedMessage = new Message();
        savedMessage.setSender("lukasz");
        savedMessage.setContent("Hej!");
        savedMessage.setGroupId(10L);
        when(messageService.save(any(Message.class))).thenReturn(savedMessage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String payload = "{\"type\":\"CHAT\",\"content\":\"Hej!\"}";
        handler.handleTextMessage(session1, new TextMessage(payload));
        verify(session2, never()).sendMessage(argThat(
                m -> m instanceof TextMessage &&
                        ((TextMessage) m).getPayload().contains("Hej!")
        ));
    }

    /**
     * Test czy po zamknieciu sesji usuwa usera z zapisu sesji userow ACTIVE
     * @throws Exception
     */
    @Test
    void afterConnectionClosed_shouldRemoveSessionFromActiveUsers() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.notifyKeyReady(1L);
        verify(session, never()).sendMessage(argThat(
                m -> m instanceof TextMessage &&
                        ((TextMessage) m).getPayload().contains("KEY_READY")
        ));
    }

    /**
     * Test czy po rozlaczeniu wiadomosci juz nie docieraja na ta sesje
     * @throws Exception
     */
    @Test
    void afterConnectionClosed_shouldRemoveSessionFromGroupChats() throws Exception {
        WebSocketSession session = connectUser(user, "ACTIVE");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        Message savedMessage = new Message();
        savedMessage.setSender("ktos");
        savedMessage.setContent("Po rozlaczeniu");
        savedMessage.setGroupId(10L);
        clearInvocations(session);
        verify(session, never()).sendMessage(any(WebSocketMessage.class));
    }
}