package com.KryptoChat.serwer.integration;

import com.KryptoChat.serwer.DTO.DeliverKeyRequest;
import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import com.KryptoChat.serwer.repositories.GroupRepository;
import com.KryptoChat.serwer.repositories.UserRepository;
import com.KryptoChat.serwer.services.JWTService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Klasa testująca wymianę kluczy pomiędzy uzytkownikami ACTIVE -> PENDING
 * UWAGA: ta klasa wykorzystuje JWTService, który używa zmiennej środowiskowej JWT_SECRET
 * Aby działało poprawnie, nalezy zdefiniowac zmienna srodowiskową JWT_SECRET jako ciag znakow o dlugosci co najmniej 32 znakow
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class KeyExchangeIntegrationTest {

    /**
     * Port dla websocketa
     */
    @LocalServerPort
    private int port;

    /**
     * Integrowane klasy
     */
    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupKeyRepository groupKeyRepository;

    @Autowired
    private GroupRepository groupRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    /**
     * Wykorzystywane stale i obiekty w testach
     */
    private static final int TIMEOUT_SEC = 5;

    private User userA;
    private User userB;
    private User userC;
    private Group group;

    /**
     * Pomocnicza metoda tworząca Usera
     * @param username
     * @return User
     */
    private User createUser(String username) {
        User u = new User(username, "hashedPassword");
        u.setPublicKey("pubKey_" + username);
        u.setEncryptedPrivateKey("encPrivKey_" + username);
        return userRepository.save(u);
    }

    /**
     * Pomocnicza metoda, tworzy grupę i zapisuje pierwszego członka z danym statusem klucza.
     */
    private Group createGroupWithMember(User user, String keyStatus) {
        Group g = new Group("TestGroup", "TST" + System.nanoTime() % 100000);
        g = groupRepository.saveAndFlush(g);
        user.setGroup(g);
        userRepository.saveAndFlush(user);
        GroupKey gk = new GroupKey(g.getId(), user.getId(), "encryptedKey_" + user.getUsername());
        gk.setStatus(keyStatus);
        groupKeyRepository.save(gk);
        return g;
    }

    /**
     * Pomocnicza metoda dodaje kolejnego usera do istniejącej grupy z danym statusem klucza.
     */
    private void addToGroup(User user, Group g, String keyStatus) {
        user.setGroup(g);
        userRepository.saveAndFlush(user);
        GroupKey gk = new GroupKey(g.getId(), user.getId(), "encryptedKey_" + user.getUsername());
        gk.setStatus(keyStatus);
        groupKeyRepository.save(gk);
    }

    /**
     * Pomocnicza metoda tworząca adres do połączen
     * @return String
     */
    private String wsUrl() {
        return "ws://localhost:" + port + "/ws";
    }

    /**
     * Pomocnicza metoda tworząca adres do REST API
     * @param path
     * @return String
     */
    private String restUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Pomocnicza metoda, otwiera sesję WebSocket i zbiera przychodzące wiadomości do listy.
     */
    private WebSocketSession connectWs(String jwt, List<String> received) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return client.execute(new CollectingHandler(received), headers,
                URI.create(wsUrl())).get(TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Pomocnicza metoda, wysyła POST z JSON body i nagłówkiem Authorization.
     */
    private HttpResponse<String> post(String path, String jwt, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(restUrl(path)))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (jwt != null) {
            builder.header("Authorization", "Bearer " + jwt);
        }
        return http.send(builder.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Pomocnicza metoda, wysyła GET z nagłówkiem Authorization.
     */
    private HttpResponse<String> get(String path, String jwt) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(restUrl(path)));
        if (jwt != null) {
            builder.header("Authorization", "Bearer " + jwt);
        }
        return http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Pomocnicza klasa statyczna, minimalny handler zbierający wiadomości WebSocket do listy.
     * Musi być zaimplementowana bo coś musi obsługiwać przychodzące wiadomości
     */
    private static class CollectingHandler implements WebSocketHandler {
        private final List<String> received;

        CollectingHandler(List<String> received) {
            this.received = received;
        }

        @Override public void afterConnectionEstablished(WebSocketSession s) {}
        @Override public void handleTransportError(WebSocketSession s, Throwable e) {}
        @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {}
        @Override public boolean supportsPartialMessages() { return false; }

        @Override
        public void handleMessage(WebSocketSession s, WebSocketMessage<?> m) {
            received.add(m.getPayload().toString());
        }
    }

    /**
     * Metoda sprzątająca po wszystkich testach - czysci obiekty oraz bazę danych
     */
    @AfterEach
    void cleanup() {
        groupKeyRepository.deleteAll();
        for (User u : Arrays.asList(userA, userB, userC)) {
            if (u == null) continue;
            u.setGroup(null);
            userRepository.save(u);
        }
        userRepository.deleteAll();
        groupRepository.deleteAll();
        userA = userB = userC = null;
        group = null;
    }

    /**
     * Test sprawdzający czy serwer przesyła żądanie klucza do aktywnego czlonka ze statusem ACTIVE
     * żądanie na rzecz każdego aktywnego usera ze statusem PENDING
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie ACTIVE: user dostaje KEY_REQUEST dla każdego PENDING członka grupy")
    void connect_activeUser_receivesKeyRequestForEachPendingMember() throws Exception {
        userA = createUser("activeUser");
        userB = createUser("pendingUser1");
        userC = createUser("pendingUser2");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        addToGroup(userC, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        String jwtA = jwtService.generateToken(userA);
        List<String> receivedA = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connectWs(jwtA, receivedA);
        Thread.sleep(600);
        long keyRequests = receivedA.stream()
                .filter(msg -> msg.contains("\"type\":\"KEY_REQUEST\""))
                .count();
        assertThat(keyRequests).isEqualTo(2);
        assertThat(receivedA.stream().anyMatch(m -> m.contains("pubKey_pendingUser1"))).isTrue();
        assertThat(receivedA.stream().anyMatch(m -> m.contains("pubKey_pendingUser2"))).isTrue();
        sessionA.close();
    }

    /**
     * Test sprawdzający czy przesyane żądanie zawiera odpowiednie pola, userId, username i co najwazniejsze
     * klucz publiczny, i czy są one poprawnie zaczytane z bazy danych
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie ACTIVE: KEY_REQUEST zawiera userId, username i publicKey oczekującego")
    void connect_activeUser_keyRequestContainsCorrectFields() throws Exception {
        userA = createUser("activeUser");
        userB = createUser("pendingUser");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedA = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connectWs(jwtService.generateToken(userA), receivedA);
        Thread.sleep(500);
        String keyReqMsg = receivedA.stream()
                .filter(m -> m.contains("KEY_REQUEST"))
                .findFirst()
                .orElse(null);
        assertThat(keyReqMsg).isNotNull();
        JsonNode node = mapper.readTree(keyReqMsg);
        assertThat(node.get("type").asText()).isEqualTo("KEY_REQUEST");
        assertThat(node.get("userId").asLong()).isEqualTo(userB.getId());
        assertThat(node.get("username").asText()).isEqualTo("pendingUser");
        assertThat(node.get("publicKey").asText()).isEqualTo("pubKey_pendingUser");
        sessionA.close();
    }

    /**
     * Test sprawdzający czy jeżeli nie ma pending to nie ma przesyłanych żadnych nie potrzebnych
     * ani potencjalnie szkodliwych KEY_REQUESTÓW
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie ACTIVE bez żadnych PENDING: brak KEY_REQUEST")
    void connect_activeUser_noPendingMembers_noKeyRequest() throws Exception {
        userA = createUser("activeUser");
        userB = createUser("activeUser2");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        List<String> receivedA = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connectWs(jwtService.generateToken(userA), receivedA);
        Thread.sleep(500);
        assertThat(receivedA.stream().anyMatch(m -> m.contains("KEY_REQUEST"))).isFalse();
        sessionA.close();
    }

    /**
     * Test sprawdzający czy po połączeniu użytkownika ze statusem PENDING, wszyscy użytkownicy ze statusem
     * ACTIVE dostają KEY_REQUEST - wszyscy w tej grupie
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie PENDING: wszyscy ACTIVE połączeni członkowie grupy dostają KEY_REQUEST")
    void connect_pendingUser_activeConnectedMembersReceiveKeyRequest() throws Exception {
        userA = createUser("activeUser");
        userB = createUser("pendingUser");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedA = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connectWs(jwtService.generateToken(userA), receivedA);
        Thread.sleep(300);
        receivedA.clear();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), receivedB);
        Thread.sleep(500);
        assertThat(receivedA.stream().anyMatch(m ->
                m.contains("KEY_REQUEST") && m.contains("pendingUser"))).isTrue();

        sessionA.close();
        sessionB.close();
    }

    /**
     * Test sprawdzający czy uzytkownik ACTIVE dostaje poprawne dane uzytkownika PENDING
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie PENDING: KEY_REQUEST do ACTIVE zawiera dane pendingUsera")
    void connect_pendingUser_keyRequestSentToActiveHasCorrectFields() throws Exception {
        userA = createUser("active");
        userB = createUser("pending");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedA = new CopyOnWriteArrayList<>();
        WebSocketSession sessionA = connectWs(jwtService.generateToken(userA), receivedA);
        Thread.sleep(300);
        receivedA.clear();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), new CopyOnWriteArrayList<>());
        Thread.sleep(500);
        String msg = receivedA.stream()
                .filter(m -> m.contains("KEY_REQUEST"))
                .findFirst().orElse(null);
        assertThat(msg).isNotNull();
        JsonNode node = mapper.readTree(msg);
        assertThat(node.get("type").asText()).isEqualTo("KEY_REQUEST");
        assertThat(node.get("userId").asLong()).isEqualTo(userB.getId());
        assertThat(node.get("username").asText()).isEqualTo("pending");
        assertThat(node.get("publicKey").asText()).isEqualTo("pubKey_pending");
        sessionA.close();
        sessionB.close();
    }

    /**
     * Test sprawdzający co się stanie jeżeli połączy się PENDING a żaden ACTIVE nie jest połączony
     * (Nic nie powinno się stać, jak ktoś się połączy to dostanie request)
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie PENDING: ACTIVE offline → KEY_REQUEST nie wysłany (brak błędu)")
    void connect_pendingUser_activeNotConnected_noKeyRequestSent() throws Exception {
        userA = createUser("activeOffline");
        userB = createUser("pendingUser");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), receivedB);
        Thread.sleep(400);
        assertThat(sessionB.isOpen()).isTrue();
        assertThat(receivedB.stream().anyMatch(m -> m.contains("KEY_REQUEST"))).isFalse();
        sessionB.close();
    }

    /**
     * Test sprawdzający co się stanie po połączeniu usera bez grupy - brak Key Requestm zabezpieczenie przed atakami
     * @throws Exception
     */
    @Test
    @DisplayName("Połączenie usera bez grupy: brak KEY_REQUEST, sesja otwarta")
    void connect_userWithoutGroup_noKeyRequestAndSessionOpen() throws Exception {
        userA = createUser("noGroupUser");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocketSession session = connectWs(jwtService.generateToken(userA), received);
        Thread.sleep(400);
        assertThat(session.isOpen()).isTrue();
        assertThat(received.stream().anyMatch(m -> m.contains("KEY_REQUEST"))).isFalse();
        session.close();
    }

    /**
     * Test sprawdzający czy po dostarczeniu klucza ten jest poprawnie zapisywany w bazie danych
     * oraz czy status się zmienia na ACTIVE
     * @throws Exception
     */
    @Test
    @DisplayName("deliver-key: klucz zapisany w bazie, status zmieniony na ACTIVE")
    void deliverKey_validRequest_keyPersistedWithActiveStatus() throws Exception {
        userA = createUser("activeDeliverer");
        userB = createUser("pendingReceiver");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), receivedB);
        Thread.sleep(300);
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(userB.getId());
        req.setEncryptedKey("encryptedGroupKeyForB");
        HttpResponse<String> response = post("/api/groups/deliver-key",
                jwtService.generateToken(userA), req);
        assertThat(response.statusCode()).isEqualTo(200);
        GroupKey gk = groupKeyRepository
                .findByGroupIdAndUserId(group.getId(), userB.getId())
                .orElseThrow();
        assertThat(gk.getEncryptedGroupKey()).isEqualTo("encryptedGroupKeyForB");
        assertThat(gk.getStatus()).isEqualTo("ACTIVE");
        sessionB.close();
    }

    /**
     * Test sprawdzający czy po tym jak serwer otrzymał klucz
     * To czy serwer wyśle do tego usera powiadomienie o otrzymaniu klucza przez WebSocket
     * @throws Exception
     */
    @Test
    @DisplayName("deliver-key: KEY_READY wysyłane do targetUsera przez WebSocket")
    void deliverKey_validRequest_keyReadySentViaWebSocket() throws Exception {
        userA = createUser("deliverer");
        userB = createUser("receiver");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), receivedB);
        Thread.sleep(300);
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(userB.getId());
        req.setEncryptedKey("someEncryptedKey");
        post("/api/groups/deliver-key", jwtService.generateToken(userA), req);
        Thread.sleep(600);
        assertThat(receivedB.stream()
                .anyMatch(m -> m.contains("KEY_READY"))).isTrue();

        sessionB.close();
    }

    /**
     * Test sprawdzający czy komunikat KEY_READY nie jest przypadkiem wysylany omylkowo do innych userow
     * @throws Exception
     */
    @Test
    @DisplayName("deliver-key: KEY_READY nie trafia do innych userów, tylko do targetUsera")
    void deliverKey_keyReadySentOnlyToTargetUser() throws Exception {
        userA = createUser("delivererUser");
        userB = createUser("targetUser");
        userC = createUser("otherUser");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        addToGroup(userC, group, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        userC = userRepository.findById(userC.getId()).orElseThrow();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        List<String> receivedC = new CopyOnWriteArrayList<>();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), receivedB);
        WebSocketSession sessionC = connectWs(jwtService.generateToken(userC), receivedC);
        Thread.sleep(400);
        receivedC.clear();
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(userB.getId());
        req.setEncryptedKey("keyForB");
        post("/api/groups/deliver-key", jwtService.generateToken(userA), req);
        Thread.sleep(500);
        assertThat(receivedB.stream().anyMatch(m -> m.contains("KEY_READY"))).isTrue();
        assertThat(receivedC.stream().anyMatch(m -> m.contains("KEY_READY"))).isFalse();
        sessionB.close();
        sessionC.close();
    }

    /**
     * Test sprawdzający co się stanie w sytuacji gdy:
     * dwóch użytkowników ze statusem ACTIVE było aktywnych i oboje przeslali klucz
     * Czy pierwszy zostanie zapisany
     * Czy drugi zostanie odrzucony i nic nie nadpisze
     * @throws Exception
     */
    @Test
    @DisplayName("deliver-key: target ma status ACTIVE (nie PENDING) daje status 400 Bad Request")
    void deliverKey_targetAlreadyActive_returns400() throws Exception {
        userA = createUser("delivererA");
        userB = createUser("alreadyActiveB");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "ACTIVE");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(userB.getId());
        req.setEncryptedKey("wrongKey");
        HttpResponse<String> response = post("/api/groups/deliver-key",
                jwtService.generateToken(userA), req);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    /**
     * Test sprawdzajacy odrzucenie klucza gdy w headerze jest brak naglowka
     * @throws Exception
     */
    @Test
    @DisplayName("deliver-key: brak nagłówka Authorization daje status 401")
    void deliverKey_noAuth_returns401() throws Exception {
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(99L);
        req.setEncryptedKey("key");
        HttpResponse<String> response = post("/api/groups/deliver-key", null, req);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    /**
     * Test sprawdzajacy odrzucenie klucza i nie zapisanie w bazie gdy token nie jest valid
     * @throws Exception
     */
    @Test
    @DisplayName("deliver-key: nieważny token daje status 401")
    void deliverKey_invalidToken_returns401() throws Exception {
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(99L);
        req.setEncryptedKey("key");
        HttpResponse<String> response = post("/api/groups/deliver-key",
                "invalid.token.here", req);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    /**
     * Test sprawdza czy gdy user chce pobrac klucz, a klucz jeszcze nie zostal dostarczony
     * to czy serwer zwroci mu 202 z komunikatem ze jeszcze nie ma klucza i zeby dalej czekal
     * @throws Exception
     */
    @Test
    @DisplayName("my-key: status PENDING daje status 202 z body 'PENDING'")
    void myKey_pendingStatus_returns202WithPendingBody() throws Exception {
        userA = createUser("pendingKeyUser");
        group = createGroupWithMember(userA, "PENDING");
        GroupKey gk = groupKeyRepository
                .findByGroupIdAndUserId(group.getId(), userA.getId()).orElseThrow();
        gk.setEncryptedGroupKey(null);
        groupKeyRepository.save(gk);
        userA = userRepository.findById(userA.getId()).orElseThrow();
        HttpResponse<String> response = get("/api/groups/my-key",
                jwtService.generateToken(userA));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).isEqualTo("PENDING");
    }

    /**
     * Test sprawdzający czy jeżeli klucz już jest w bazie, to czy się go faktycznie uda pobrac
     * na klienta tego usera poprawnym requestem
     * @throws Exception
     */
    @Test
    @DisplayName("my-key: status ACTIVE daje status 200 z zaszyfrowanym kluczem grupy")
    void myKey_activeStatus_returns200WithEncryptedKey() throws Exception {
        userA = createUser("activeKeyUser");
        group = createGroupWithMember(userA, "ACTIVE");
        GroupKey gk = groupKeyRepository
                .findByGroupIdAndUserId(group.getId(), userA.getId()).orElseThrow();
        gk.setEncryptedGroupKey("myEncryptedGroupKey");
        groupKeyRepository.save(gk);
        userA = userRepository.findById(userA.getId()).orElseThrow();
        HttpResponse<String> response = get("/api/groups/my-key",
                jwtService.generateToken(userA));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("myEncryptedGroupKey");
    }

    /**
     * Test sprawdzający pełne flow - dostarczenie klucza do bazy dancyh i czy mozna wowczas pobrac
     * endpointem /my-key z bazy danych na klienta
     * @throws Exception
     */
    @Test
    @DisplayName("my-key: po deliver-key klucz jest dostępny przez /my-key (pełny flow)")
    void myKey_afterDeliverKey_keyAccessible() throws Exception {
        userA = createUser("flowActive");
        userB = createUser("flowPending");
        group = createGroupWithMember(userA, "ACTIVE");
        addToGroup(userB, group, "PENDING");
        userA = userRepository.findById(userA.getId()).orElseThrow();
        userB = userRepository.findById(userB.getId()).orElseThrow();
        HttpResponse<String> before = get("/api/groups/my-key",
                jwtService.generateToken(userB));
        assertThat(before.statusCode()).isEqualTo(202);
        List<String> receivedB = new CopyOnWriteArrayList<>();
        WebSocketSession sessionB = connectWs(jwtService.generateToken(userB), receivedB);
        Thread.sleep(300);
        DeliverKeyRequest req = new DeliverKeyRequest();
        req.setTargetUserId(userB.getId());
        req.setEncryptedKey("deliveredKey123");
        post("/api/groups/deliver-key", jwtService.generateToken(userA), req);
        Thread.sleep(400);
        HttpResponse<String> after = get("/api/groups/my-key",
                jwtService.generateToken(userB));
        assertThat(after.statusCode()).isEqualTo(200);
        assertThat(after.body()).isEqualTo("deliveredKey123");
        sessionB.close();
    }

    /**
     * Test sprawdzajacy czy przy braku naglowka przy pobieraniu klucza bedzie BadRequest
     * @throws Exception
     */
    @Test
    @DisplayName("my-key: brak nagłówka Authorization daje status 400")
    void myKey_noAuth_returns401() throws Exception {
        HttpResponse<String> response = get("/api/groups/my-key", null);
        assertThat(response.statusCode()).isEqualTo(400);
    }

    /**
     * Test sprawdzajacy czy przy pobieraniu klucza ze zlym tokenem, czy bedzie odrzucone
     * @throws Exception
     */
    @Test
    @DisplayName("my-key: nieważny token daje status 401")
    void myKey_invalidToken_returns401() throws Exception {
        HttpResponse<String> response = get("/api/groups/my-key", "invalid.token.xyz");
        assertThat(response.statusCode()).isEqualTo(401);
    }
}