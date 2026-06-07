package com.KryptoChat.serwer.integration;

import com.KryptoChat.serwer.DTO.*;
import com.KryptoChat.serwer.repositories.*;
import com.KryptoChat.serwer.entities.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testy integracyjne dla:
 *  - flow zaczytywania członków grupy z bazy danych (GET /api/groups/)
 *  - flow zaczytywania wiadomości grupy z bazy danych (GET /api/messages/)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class GroupAndMessageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private static final String CREATOR_USERNAME = "tworca";
    private static final String MEMBER_USERNAME_1 = "czlonek1";
    private static final String MEMBER_USERNAME_2 = "czlonek2";
    private static final String PASSWORD = "Test1234!";
    private static final String PUB_KEY = "publicKeyRSA";
    private static final String ENC_KEY = "encryptedPrivateKey";
    private static final String GROUP_NAME = "TestowaGrupa";
    private static final String CREATOR_KEY = "creatorEncryptedGroupKey";

    /**
     * Buduje RegisterRequest z podaną nazwą użytkownika i domyślnymi pozostałymi polami.
     */
    private RegisterRequest buildRegisterRequest(String username) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(PASSWORD);
        req.setPublicKey(PUB_KEY);
        req.setEncryptedPrivateKey(ENC_KEY);
        return req;
    }

    /**
     * Rejestruje użytkownika i zwraca JWT uzyskany po logowaniu.
     */
    private String registerAndLogin(String username) throws Exception {
        RegisterRequest req = buildRegisterRequest(username);

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk()).andReturn();

        String body = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("jwt").asText();
    }

    /**
     * Tworzy grupę jako podany użytkownik (przez JWT) i zwraca nowy JWT
     * zawierający już przypisane groupId.
     */
    private String createGroup(String jwt, String groupName) throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setGroupName(groupName);
        req.setCreatorKey(CREATOR_KEY);

        MvcResult result = mockMvc.perform(post("/api/groups/create").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("jwt").asText();
    }

    /**
     * Wywołuje flush + clear na EntityManager, żeby wymusić przeładowanie
     * stanu bazy przy następnym zapytaniu Hibernate.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Pobiera kod zaproszenia grupy, do której należy użytkownik wskazany przez JWT.
     */
    private String fetchGroupCode(String jwt) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/groups/").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk()).andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asText();
    }

    /**
     * Dołącza użytkownika (wskazanego przez JWT) do grupy o danym kodzie
     * i zwraca nowy JWT z przypisanym groupId.
     */
    private String joinGroup(String jwt, String code) throws Exception {
        JoinGroupRequest req = new JoinGroupRequest();
        req.setCode(code);

        MvcResult result = mockMvc.perform(post("/api/groups/join")
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk()).andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("jwt").asText();
    }

    /**
     * Scenariusz: twórca grupy tworzy grupę → endpoint zwraca go jako jedynego
     * członka, a dane pokrywają się z rekordem w bazie danych.
     */
    @Test
    @DisplayName("Pobieranie członków: twórca jest jedynym członkiem po utworzeniu grupy")
    void getGroupMembers_afterCreation_onlyCreatorReturned() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        flushAndClear();

        mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.users[0].username").value(CREATOR_USERNAME));
    }

    /**
     * Scenariusz: po dołączeniu dwóch dodatkowych użytkowników endpoint
     * zwraca trzech członków (twórcę + 2 członków).
     */
    @Test
    @DisplayName("Pobieranie członków: po dołączeniu dwóch użytkowników lista zawiera trzech członków")
    void getGroupMembers_afterTwoMembersJoin_allThreeReturned() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt = createGroup(creatorJwt, GROUP_NAME);
        String code = fetchGroupCode(groupJwt);

        String member1Jwt = registerAndLogin(MEMBER_USERNAME_1);
        joinGroup(member1Jwt, code);

        String member2Jwt = registerAndLogin(MEMBER_USERNAME_2);
        joinGroup(member2Jwt, code);

        flushAndClear();

        mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(3));
    }

    /**
     * Scenariusz: usernames zwrócone przez endpoint pokrywają się dokładnie
     * z tymi zapisanymi w bazie danych.
     */
    @Test
    @DisplayName("Pobieranie członków: nazwy użytkowników zgodne z bazą danych")
    void getGroupMembers_usernamesMatchDatabase() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt = createGroup(creatorJwt, GROUP_NAME);
        String code = fetchGroupCode(groupJwt);

        String member1Jwt = registerAndLogin(MEMBER_USERNAME_1);
        joinGroup(member1Jwt, code);

        flushAndClear();

        MvcResult result = mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + groupJwt)).andExpect(status().isOk()).andReturn();

        JsonNode users = objectMapper.readTree(result.getResponse().getContentAsString()).get("users");

        List<String> returnedUsernames = new ArrayList<>();
        users.forEach(u -> returnedUsernames.add(u.get("username").asText()));

        assertThat(returnedUsernames).containsExactlyInAnyOrder(CREATOR_USERNAME, MEMBER_USERNAME_1);
    }


    /**
     * Scenariusz: odpowiedź zawiera prawidłową nazwę grupy oraz jej id.
     */
    @Test
    @DisplayName("Pobieranie członków: odpowiedź zawiera poprawną nazwę grupy i groupId")
    void getGroupMembers_responseContainsCorrectGroupNameAndId() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        Long dbGroupId = groupRepository.findAll().stream()
                .filter(g -> GROUP_NAME.equals(g.getGroupName())).findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value(GROUP_NAME))
                .andExpect(jsonPath("$.groupId").value(dbGroupId));
    }

    /**
     * Scenariusz: brak nagłówka Authorization skutkuje odrzuceniem żądania (401).
     */
    @Test
    @DisplayName("Pobieranie członków: brak tokenu - 401 Unauthorized")
    void getGroupMembers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/groups/")).andExpect(status().is4xxClientError());
    }

    /**
     * Scenariusz: nieprawidłowy token (losowy ciąg) skutkuje odrzuceniem (401).
     */
    @Test
    @DisplayName("Pobieranie członków: nieprawidłowy token - 401 Unauthorized")
    void getGroupMembers_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer to.nie.jest.token")).andExpect(status().isUnauthorized());
    }

    /**
     * Scenariusz: użytkownik, który jeszcze nie dołączył do żadnej grupy,
     * nie może pobrać listy członków — serwer rzuca wyjatek.
     */
    @Test
    @DisplayName("Pobieranie członków: user bez grupy - wyjatek")
    void getGroupMembers_userWithoutGroup_returnsServerError() throws Exception {
        String jwt = registerAndLogin(CREATOR_USERNAME);

        ServletException ex = assertThrows(ServletException.class, () -> mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + jwt)));

        assertTrue(ex.getCause() instanceof NullPointerException);
    }

    /**
     * Scenariusz: kod zaproszenia zwrócony przez endpoint jest niepusty
     * i pokrywa się z tym zapisanym w bazie danych.
     */
    @Test
    @DisplayName("Pobieranie członków: kod grupy w odpowiedzi zgodny z bazą danych")
    void getGroupMembers_groupCodeMatchesDatabase() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        MvcResult result = mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + groupJwt)).andExpect(status().isOk()).andReturn();

        String returnedCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("code").asText();

        String dbCode = groupRepository.findAll().stream().filter(g -> GROUP_NAME.equals(g.getGroupName()))
                .findFirst().orElseThrow().getKod();

        assertThat(returnedCode).isNotBlank();
        assertThat(returnedCode).isEqualTo(dbCode);
    }

    /**
     * Scenariusz: pełny przepływ — rejestracja, tworzenie grupy, dołączenie
     * drugiego użytkownika → pobrana lista spójna z danymi obu rekordów
     * w bazie danych.
     */
    @Test
    @DisplayName("Pobieranie członków: pełny przepływ — dane spójne z bazą danych")
    void getGroupMembers_fullFlow_dataConsistentWithDatabase() throws Exception {

        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt = createGroup(creatorJwt, GROUP_NAME);
        String code   = fetchGroupCode(groupJwt);

        String memberJwt = registerAndLogin(MEMBER_USERNAME_1);
        joinGroup(memberJwt, code);

        entityManager.flush();
        entityManager.clear();

        Long dbCreatorId = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow().getId();
        Long dbMemberId = userRepository.findByUsername(MEMBER_USERNAME_1).orElseThrow().getId();

        MvcResult result = mockMvc.perform(get("/api/groups/")
                        .header("Authorization", "Bearer " + groupJwt)).andExpect(status().isOk()).andReturn();

        JsonNode users = objectMapper.readTree(result.getResponse().getContentAsString()).get("users");

        List<Long> returnedIds = new ArrayList<>();
        users.forEach(u -> returnedIds.add(u.get("id").asLong()));

        assertThat(returnedIds).containsExactlyInAnyOrder(dbCreatorId, dbMemberId);
    }


    /**
     * Scenariusz: po utworzeniu grupy, gdy żadna wiadomość nie została wysłana,
     * endpoint zwraca pustą listę.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: nowa grupa bez wiadomości - pusta lista")
    void getMessages_newGroup_emptyListReturned() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + groupJwt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(0));
    }

    /**
     * Scenariusz: po zapisaniu wiadomości bezpośrednio do repozytorium
     *  endpoint pobiera ją i zwraca w odpowiedzi.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: wiadomość zapisana w bazie - widoczna w odpowiedzi")
    void getMessages_messageSavedInDb_returnedByEndpoint() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        User   sender = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
        Group  group  = groupRepository.findAll().stream()
                .filter(g -> GROUP_NAME.equals(g.getGroupName())).findFirst().orElseThrow();

        Message msg = new Message();
        msg.setContent("Testowa wiadomosc");
        msg.setSender(sender.getUsername());
        msg.setGroupId(group.getId());
        messageRepository.save(msg);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].content").value("Testowa wiadomosc"));
    }

    /**
     * Scenariusz: wiele wiadomości zapisanych w bazie danych jest zwracanych
     * przez endpoint, a ich liczba jest zgodna z liczbą w bazie.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: wiele wiadomości — liczba zgodna z bazą")
    void getMessages_multipleMessages_countMatchesDatabase() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        User  sender = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
        Group group  = groupRepository.findAll().stream()
                .filter(g -> GROUP_NAME.equals(g.getGroupName())).findFirst().orElseThrow();

        int expectedCount = 5;
        for (int i = 1; i <= expectedCount; i++) {
            Message msg = new Message();
            msg.setContent("Wiadomosc " + i);
            msg.setSender(sender.getUsername());
            msg.setGroupId(group.getId());
            messageRepository.save(msg);
        }

        assertThat(messageRepository.findByGroupId(group.getId())).hasSize(expectedCount);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(expectedCount));
    }

    /**
     * Scenariusz: nadawca (senderId / senderUsername) zwrócony w odpowiedzi
     * jest zgodny z danymi zapisanymi w bazie danych.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: dane nadawcy zgodne z bazą danych")
    void getMessages_senderDataMatchesDatabase() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        User  sender = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
        Group group  = groupRepository.findAll().stream()
                .filter(g -> GROUP_NAME.equals(g.getGroupName())).findFirst().orElseThrow();

        Message msg = new Message();
        msg.setContent("Wiadomosc od tworcy");
        msg.setSender(sender.getUsername());
        msg.setGroupId(group.getId());
        messageRepository.save(msg);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + groupJwt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].sender").value(CREATOR_USERNAME));
    }

    /**
     * Scenariusz: wiadomości należące do innej grupy nie są widoczne
     * w odpowiedzi dla aktualnej grupy (izolacja danych).
     */
    @Test
    @DisplayName("Pobieranie wiadomości: wiadomości innych grup nie są widoczne")
    void getMessages_messagesFromOtherGroup_notReturned() throws Exception {
        // twórca tworzy pierwszą grupę
        String creator1Jwt = registerAndLogin(CREATOR_USERNAME);
        String group1Jwt   = createGroup(creator1Jwt, GROUP_NAME);

        // drugi użytkownik tworzy drugą grupę
        String creator2Jwt = registerAndLogin(MEMBER_USERNAME_1);
        String group2Jwt   = createGroup(creator2Jwt, "DrugaGrupa");

        User  sender2 = userRepository.findByUsername(MEMBER_USERNAME_1).orElseThrow();
        Group group2  = groupRepository.findAll().stream()
                .filter(g -> "DrugaGrupa".equals(g.getGroupName()))
                .findFirst().orElseThrow();

        Message msgGroup2 = new Message();
        msgGroup2.setContent("Wiadomosc tylko dla grupy 2");
        msgGroup2.setSender(sender2.getUsername());
        msgGroup2.setGroupId(group2.getId());
        messageRepository.save(msgGroup2);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + group1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(0));
    }

    /**
     * Scenariusz: wiadomości z bazy danych zawierają zaszyfrowaną treść
     * (treść zwrócona przez endpoint jest taka sama jak zapisana w bazie).
     */
    @Test
    @DisplayName("Pobieranie wiadomości: treść wiadomości zgodna z zapisem w bazie danych")
    void getMessages_encryptedContent_matchesDatabase() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt   = createGroup(creatorJwt, GROUP_NAME);

        User  sender = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
        Group group  = groupRepository.findAll().stream()
                .filter(g -> GROUP_NAME.equals(g.getGroupName())).findFirst().orElseThrow();

        String encryptedContent = "BASE64_ENCRYPTED_PAYLOAD==";
        Message msg = new Message();
        msg.setContent(encryptedContent);
        msg.setSender(sender.getUsername());
        msg.setGroupId(group.getId());
        messageRepository.save(msg);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content").value(encryptedContent));
    }

    /**
     * Scenariusz: brak nagłówka Authorization przy pobieraniu wiadomości - status 401.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: brak tokenu - 401 Unauthorized")
    void getMessages_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/messages/"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Scenariusz: nieprawidłowy token przy pobieraniu wiadomości - status 401.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: nieprawidłowy token - 401 Unauthorized")
    void getMessages_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer nieprawidlowytoken123"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Scenariusz: user bez przypisanej grupy próbuje pobrać wiadomości
     * → serwer rzuca wyjatek
     * @throws Exception
     */
    @Test
    void getMessages_userWithoutGroup_returnsNPE() throws Exception {

        String jwt = registerAndLogin(CREATOR_USERNAME);

        ServletException ex = assertThrows(ServletException.class, () -> mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer " + jwt)));

        assertTrue(ex.getCause() instanceof NullPointerException);
    }

    /**
     * Scenariusz: pełny przepływ — rejestracja, tworzenie grupy, dołączenie
     * drugiego użytkownika, zapisanie wiadomości przez obu - obie wiadomości
     * widoczne, dane spójne z bazą danych.
     */
    @Test
    @DisplayName("Pobieranie wiadomości: pełny przepływ — dane spójne z bazą danych")
    void getMessages_fullFlow_dataConsistentWithDatabase() throws Exception {
        String creatorJwt = registerAndLogin(CREATOR_USERNAME);
        String groupJwt = createGroup(creatorJwt, GROUP_NAME);
        String code = fetchGroupCode(groupJwt);

        String memberJwt = registerAndLogin(MEMBER_USERNAME_1);
        joinGroup(memberJwt, code);

        User  creator = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
        User  member= userRepository.findByUsername(MEMBER_USERNAME_1).orElseThrow();
        Group group = groupRepository.findAll().stream()
                .filter(g -> GROUP_NAME.equals(g.getGroupName())).findFirst().orElseThrow();

        Message msg1 = new Message();
        msg1.setContent("Wiadomosc od tworcy");
        msg1.setSender(creator.getUsername());
        msg1.setGroupId(group.getId());
        messageRepository.save(msg1);

        Message msg2 = new Message();
        msg2.setContent("Wiadomosc od czlonka");
        msg2.setSender(member.getUsername());
        msg2.setGroupId(group.getId());
        messageRepository.save(msg2);

        assertThat(messageRepository.findByGroupId(group.getId())).hasSize(2);

        MvcResult result = mockMvc.perform(get("/api/messages/").header("Authorization", "Bearer " + groupJwt))
                .andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()")
                        .value(2)).andReturn();

        JsonNode messages = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("messages");

        List<String> returnedContents = new ArrayList<>();
        messages.forEach(m -> returnedContents.add(m.get("content").asText()));

        assertThat(returnedContents).containsExactlyInAnyOrder("Wiadomosc od tworcy", "Wiadomosc od czlonka");
    }
}
