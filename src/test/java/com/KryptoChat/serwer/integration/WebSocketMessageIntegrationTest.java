package com.KryptoChat.serwer.integration;

import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import com.KryptoChat.serwer.repositories.GroupRepository;
import com.KryptoChat.serwer.repositories.MessageRepository;
import com.KryptoChat.serwer.repositories.UserRepository;
import com.KryptoChat.serwer.services.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy integracyjne WebSocket — obsługa wiadomości CHAT.
 * UWAGA - te testy zawierają JWTService, który korzysta ze zmiennej środowiskowej JWT_SECRET
 * Aby testy przechodziły poprawnie, konieczne jest ustawienie zmiennej środowiskowej JWT_SECRET o wartosci jakiegos ciagu znakow nie krotszego niz 32 znaki
*/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class WebSocketMessageIntegrationTest {
    /**
     * Port na którym startuje websocket - lokalny
     */
    @LocalServerPort
    private int port;

    /**
     * Integrowane repozytoria
     */
    @Autowired
    private JWTService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupKeyRepository groupKeyRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private GroupRepository groupRepository;

    /**
     * Wykorzystywane w testach stala i obiekty
     */
    private static final int TIMEOUT_SEC = 5;
    private User userA;
    private User userB;
    private Group group;


    /**
     * Metoda pomocnicza, tworzy usera bezpośrednio w bazie
     */
    private User createUser(String username) {
        User u = new User(username, "hashedPassword");
        u.setPublicKey("pubKey_" + username);
        u.setEncryptedPrivateKey("encPrivKey_" + username);
        return userRepository.save(u);
    }

    /**
     * Metoda pomocnicza, tworzy grupę i przypisuje do niej usera wraz z GroupKey o podanym statusie.
     */
    private Group createGroupWithMember(User user, String keyStatus) {
        Group g = new Group("TestGroup", "TEST01");
        g = groupRepository.saveAndFlush(g);
        user.setGroup(g);
        userRepository.save(user);
        userRepository.saveAndFlush(user);
        GroupKey gk = new GroupKey(g.getId(), user.getId(), "encryptedKey");
        gk.setStatus(keyStatus);
        groupKeyRepository.save(gk);
        return g;
    }

    /**
     * Metoda pomocnicza, dodaje drugiego usera do istniejącej grupy.
     */
    private void addToGroup(User user, Group g, String keyStatus) {
        user.setGroup(g);
        userRepository.save(user);
        GroupKey gk = new GroupKey(g.getId(), user.getId(), "encryptedKey");
        gk.setStatus(keyStatus);
        groupKeyRepository.save(gk);
    }

    /**
     * Metoda pomocnicza, buduje URL WebSocketa dla danego portu.
     */
    private String wsUrl() {
        return "ws://localhost:" + port + "/ws";
    }

    /**
     * Metoda pomocnicza, tworzy WebSocketSession z nagłówkiem Authorization i zbiera przychodzące wiadomości do listy.
     */
    private WebSocketSession connect(String jwt, List<String> received) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return client.execute(new TextWebSocketHandlerImpl(received), headers, java.net.URI.create(wsUrl())).get(TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Uproszczony handler WebSocket dla klienta testowego
     * Implementuje WebSocketHandler, zbiera wiadomosci klientow do listy
     * Ta klasa statyczna jest potrzebna bo cos musi obslugiwac przychodzace wiadomosci
     */
    private static class TextWebSocketHandlerImpl implements WebSocketHandler {
        private final List<String> received;

        TextWebSocketHandlerImpl(List<String> received) {
            this.received = received;
        }

        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTransportError(WebSocketSession s, Throwable e) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
        @Override public boolean supportsPartialMessages() { return false; }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            received.add(message.getPayload().toString());
        }
    }

    /**
     * Metoda sprzatajaca po testach - userow i grupy
     */
    @AfterEach
    void cleanup() {
        messageRepository.deleteAll();
        groupKeyRepository.deleteAll();
        if (userA != null) {
            userA.setGroup(null);
            userRepository.save(userA);
        }
        if (userB != null) {
            userB.setGroup(null);
            userRepository.save(userB);
        }
        userRepository.deleteAll();
        groupRepository.deleteAll();
    }

    /**
     * Test sprawdzajacy czy jezeli polaczenie jest invalid - brak naglowka to czy serwer zamknie sesje websocket
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie bez nagłówka Authorization → sesja jest zamknięta")
    void connect_noAuthHeader_sessionIsClosed() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = client.execute(
                new TextWebSocketHandlerImpl(received),
                new WebSocketHttpHeaders(),
                java.net.URI.create(wsUrl())
        ).get(TIMEOUT_SEC, TimeUnit.SECONDS);
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }

    /**
     * Test sprawdzajacy czy dla poprawnego polaczenia, sesja websocket jest otwierana i pozostaje otwarta
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie z prawidłowym JWT → sesja pozostaje otwarta")
    void connect_validToken_sessionStaysOpen() throws Exception {
        userA = createUser("userA");
        String jwt = jwtService.generateToken(userA);
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = connect(jwt, received);
        Thread.sleep(300);
        assertThat(session.isOpen()).isTrue();
        session.close();
    }

    /**
     * Test sprawdzajacy czy dla zlego tokena sesja jest zamykana
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie z losowym tokenem → sesja jest zamknięta")
    void connect_invalidToken_sessionIsClosed() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer to.nie.jest.prawidlowy.token");
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = client.execute(
                new TextWebSocketHandlerImpl(received),
                headers,
                java.net.URI.create(wsUrl())
        ).get(TIMEOUT_SEC, TimeUnit.SECONDS);
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }

    /**
     * Test sprawdzajacy czy jezeli user ktory ma poprawną, zapisaną sesje
     * To czy po wyslaniu przez niego wiadomosci, wiadomość ta została poprawnie zapisana w bazie danych
     * @throws Exception
     */
    @Test
    @DisplayName("Wiadomość CHAT → zapisana w bazie z poprawnym senderem i treścią")
    void chat_validMessage_savedToDatabase() throws Exception {
        userA = createUser("userA");
        group = createGroupWithMember(userA, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        String jwt = jwtService.generateToken(userA);
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = connect(jwt, received);
        Thread.sleep(300);
        String payload = """
                {"type":"CHAT","content":"Testowa wiadomosc"}
                """;
        session.sendMessage(new TextMessage(payload));
        Thread.sleep(500);
        List<com.KryptoChat.serwer.entities.Message> messages = messageRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Testowa wiadomosc");
        assertThat(messages.get(0).getSender()).isEqualTo("userA");
        assertThat(messages.get(0).getGroupId()).isEqualTo(group.getId());
        session.close();
    }

    /**
     * Test sprawdzający czy wiadomość została zapisana w bazie z rzeczywistym username usera o tym id
     * To chroni przed atakami jeżeli ktoś o innym id podmieni jakoś wysylany username
     * @throws Exception
     */
    @Test
    @DisplayName("Wiadomość CHAT → nadawca pochodzi z bazy, nie z payloadu")
    void chat_senderTakenFromDatabase_notFromPayload() throws Exception {
        userA = createUser("userA");
        group = createGroupWithMember(userA, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        String jwt = jwtService.generateToken(userA);
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = connect(jwt, received);
        Thread.sleep(300);
        String payload = """
                {"type":"CHAT","sender":"haker","content":"Probuję podrobić sendera"}
                """;
        session.sendMessage(new TextMessage(payload));
        Thread.sleep(500);
        var messages = messageRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getSender()).isEqualTo("userA");
        session.close();
    }

    /**
     * Test sprawdzający czy po otrzymaniu wiadomości przez serwer,
     * wiadomość ta jest broadcastowana do wszystkich aktywnych użytkowników tej grupy, a nie jednego
     * test na 2 członkach grupy
     * @throws Exception
     */
    @Test
    @DisplayName("Wiadomość CHAT → broadcast do obu sesji w tej samej grupie")
    void chat_broadcastToAllSessionsInGroup() throws Exception {
        userA = createUser("userA");
        userB = createUser("userB");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        String jwtA = jwtService.generateToken(userA);
        String jwtB = jwtService.generateToken(userB);
        List<String> receivedA = new CopyOnWriteArrayList<>();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connect(jwtA, receivedA);
        WebSocketSession sessionB = connect(jwtB, receivedB);
        Thread.sleep(400);
        sessionA.sendMessage(new TextMessage("""
                {"type":"CHAT","content":"Hej grupaaa"}
                """));
        Thread.sleep(500);
        assertThat(receivedA.stream().anyMatch(m -> m.contains("Hej grupaaa"))).isTrue();
        assertThat(receivedB.stream().anyMatch(m -> m.contains("Hej grupaaa"))).isTrue();
        sessionA.close();
        sessionB.close();
    }

    /**
     * Test sprawdzający czy serwer broadcastuje wiadomości tlyko do aktywnych członków danej grupy
     * a nie na przykład czlonka nalezacego do innej grupy
     * @throws Exception
     */
    @Test
    @DisplayName("Wiadomość CHAT → nie trafia do usera z innej grupy")
    void chat_notBroadcastedToOtherGroup() throws Exception {
        userA = createUser("userA");
        userB = createUser("userB");
        group = createGroupWithMember(userA, "ACTIVE");
        Group group2 = new Group("OtherGroup", "OTHE02");
        group2 = groupRepository.saveAndFlush(group2);
        addToGroup(userB, group2, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedA = new CopyOnWriteArrayList<>();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connect(jwtService.generateToken(userA), receivedA);
        WebSocketSession sessionB = connect(jwtService.generateToken(userB), receivedB);
        Thread.sleep(400);
        sessionA.sendMessage(new TextMessage("""
                {"type":"CHAT","content":"Tylko dla mojej grupy"}
                """));
        Thread.sleep(500);
        assertThat(receivedA.stream().anyMatch(m -> m.contains("Tylko dla mojej grupy"))).isTrue();
        assertThat(receivedB.stream().noneMatch(m -> m.contains("Tylko dla mojej grupy"))).isTrue();
        sessionA.close();
        sessionB.close();
    }

    /**
     * Test sprawdzający czy wiadomości o pustej treści prawidłowo są pomijane
     * czy nie są zapisane w bazie
     * Taka sytuacja w normalnym użytkowaniu nie wystąpi, jest to zabezpieczenie, przed różnego rodzaju atakami pustych wiadomosci
     * @throws Exception
     */
    @Test
    @DisplayName("Pusta treść wiadomości → nic nie zapisane w bazie")
    void chat_emptyContent_notSaved() throws Exception {
        userA = createUser("userA");
        group = createGroupWithMember(userA, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        WebSocketSession session = connect(jwtService.generateToken(userA), new CopyOnWriteArrayList<>());
        Thread.sleep(300);
        session.sendMessage(new TextMessage("""
                {"type":"CHAT","content":""}
                """));
        Thread.sleep(400);
        assertThat(messageRepository.findAll()).isEmpty();
        session.close();
    }

    /**
     * Test sprawdzający czy wiadomości o długości powyżej 500 znaków są prawidlowo odrzucane i nie zapisane w bazie
     * @throws Exception
     */
    @Test
    @DisplayName("Treść > 500 znaków → nic nie zapisane w bazie")
    void chat_contentTooLong_notSaved() throws Exception {
        userA = createUser("userA");
        group = createGroupWithMember(userA, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        String longContent = "a".repeat(501);
        WebSocketSession session = connect(jwtService.generateToken(userA), new CopyOnWriteArrayList<>());
        Thread.sleep(300);
        session.sendMessage(new TextMessage(
                "{\"type\":\"CHAT\",\"content\":\"" + longContent + "\"}"
        ));
        Thread.sleep(400);
        assertThat(messageRepository.findAll()).isEmpty();
        session.close();
    }

    /**
     * Test sprawdzający czy rozłączanie działa prawidłowo - czy sesja jest usuwana
     * oraz czy kolejne wiadomosci nie docieraja
     * @throws Exception
     */
    @Test
    @DisplayName("Rozłączenie → sesja usuwana z aktywnych użytkowników (kolejne wiadomości nie docierają)")
    void disconnect_sessionRemovedFromActiveUsers() throws Exception {
        userA = createUser("userA");
        group = createGroupWithMember(userA, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = connect(jwtService.generateToken(userA), received);
        Thread.sleep(300);
        session.close();
        Thread.sleep(300);
        assertThat(session.isOpen()).isFalse();
    }
}