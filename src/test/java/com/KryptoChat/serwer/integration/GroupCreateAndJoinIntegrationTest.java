package com.KryptoChat.serwer.integration;

import com.KryptoChat.serwer.DTO.*;
import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.*;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.*;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne dla flow tworzenia i dołączania do grup:
 * - POST /api/groups/create
 * - POST /api/groups/join
 *
 * Każdy test działa w osobnej transakcji i jest izolowany od pozostałych.
 * UWAGA: wymagana zmienna środowiskowa JWT_SECRET (min. 32 znaki).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class GroupCreateAndJoinIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GroupCreateAndJoinIntegrationTest.class);
    @Autowired
        private MockMvc mockMvc;

        /**
        * Integrowane zaleznosci - repozytoria
        */
        @Autowired
        private UserRepository userRepository;

        @Autowired
        private GroupRepository groupRepository;

        @Autowired
        private GroupKeyRepository groupKeyRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @PersistenceContext
        private EntityManager entityManager;


        /**
        * Wykorzystywane w testach stale
        */
        private static final String CREATOR_USERNAME = "tworca";
        private static final String MEMBER_USERNAME = "czlonek";
        private static final String PASSWORD = "Test1234!";
        private static final String PUB_KEY = "publicKeyRSA";
        private static final String ENC_KEY = "encryptedPrivateKey";
        private static final String GROUP_NAME = "TestowaGrupa";
        private static final String CREATOR_KEY = "creatorEncryptedGroupKey";


        /**
         * Buduje poprawny RegisterRequest z podaną nazwą użytkownika.
         * @param username
         * @return RegisterRequest
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
         * @param username
         * @return String
         */
        private String registerAndLogin(String username) throws Exception {
            RegisterRequest req = buildRegisterRequest(username);
            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
            MvcResult loginResult = mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk()).andReturn();
            return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("jwt").asText();
        }

        /**
         * Tworzy grupę i zwraca nowy JWT (już z groupId w claims).
         * @param jwt
         * @param groupName
         * @return String
         */
        private String createGroup(String jwt, String groupName) throws Exception {
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(groupName);
            req.setCreatorKey(CREATOR_KEY);
            MvcResult result = mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk()).andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString()).get("jwt").asText();
        }

        /**
         * Pobiera kod zaproszenia z GET /api/groups/.
         * @param jwt
         * @return String
         * @throws Exception
         */
        private String fetchGroupCode(String jwt) throws Exception {
            MvcResult result = mockMvc.perform(get("/api/groups/")
                            .header("Authorization", "Bearer " + jwt)).andExpect(status().isOk()).andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asText();
        }

        /**
         * Wymusza przeładowanie stanu z bazy
         */
        private void flushAndClear() {
            entityManager.flush();
            entityManager.clear();
        }

        /**
         * Scenariusz: poprawny JWT i prawidłowa nazwa grupy daje status sukcesu 200, nowy JWT w odpowiedzi.
         */
        @Test
        @DisplayName("Tworzenie grupy: poprawne dane daje status sukcesu 200 i nowy JWT w odpowiedzi")
        void createGroup_validRequest_returns200WithJwt() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(GROUP_NAME);
            req.setCreatorKey(CREATOR_KEY);
            mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwt").isNotEmpty())
                    .andExpect(jsonPath("$.message").value("Uwtorzono grupe"));
        }

        /**
         * Scenariusz: po utworzeniu grupy, rekord grupy istnieje w bazie danych
         * z prawidłową nazwą.
         */
        @Test
        @DisplayName("Tworzenie grupy: grupa zapisana w bazie z prawidłową nazwą")
        void createGroup_validRequest_groupPersistedInDatabase() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            createGroup(jwt, GROUP_NAME);
            flushAndClear();
            assertThat(groupRepository.findByGroupName(GROUP_NAME)).isPresent();
        }

        /**
         * Scenariusz: po utworzeniu grupy twórca ma przypisane groupId w bazie danych.
         */
        @Test
        @DisplayName("Tworzenie grupy: twórca ma przypisaną grupę w bazie danych")
        void createGroup_validRequest_creatorAssignedToGroupInDatabase() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            createGroup(jwt, GROUP_NAME);
            flushAndClear();
            User creator = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
            assertThat(creator.getGroup()).isNotNull();
            assertThat(creator.getGroup().getGroupName()).isEqualTo(GROUP_NAME);
        }

        /**
         * Scenariusz: zwrócony JWT zawiera groupId zgodne z rekordem grupy w bazie.
         */
        @Test
        @DisplayName("Tworzenie grupy: userCredentials w odpowiedzi zawiera prawidłowe groupId")
        void createGroup_validRequest_responseContainsCorrectGroupId() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(GROUP_NAME);
            req.setCreatorKey(CREATOR_KEY);
            MvcResult result = mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();
            flushAndClear();
            Long responseGroupId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("userCredentials").get("groupId").asLong();
            Long dbGroupId = groupRepository.findByGroupName(GROUP_NAME).orElseThrow().getId();
            assertThat(responseGroupId).isEqualTo(dbGroupId);
        }

        /**
         * Scenariusz: po utworzeniu grupy istnieje rekord GroupKey dla twórcy
         * ze statusem ACTIVE i z przekazanym kluczem.
         */
        @Test
        @DisplayName("Tworzenie grupy: GroupKey twórcy zapisany w bazie ze statusem ACTIVE i kluczem")
        void createGroup_validRequest_creatorGroupKeyPersistedAsActive() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            createGroup(jwt, GROUP_NAME);
            flushAndClear();
            User creator = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
            Long groupId = creator.getGroup().getId();
            Optional<GroupKey> gkOpt = groupKeyRepository.findByGroupIdAndUserId(groupId, creator.getId());
            assertThat(gkOpt).isPresent();
            GroupKey gk = gkOpt.get();
            assertThat(gk.getStatus()).isEqualTo("ACTIVE");
            assertThat(gk.getEncryptedGroupKey()).isEqualTo(CREATOR_KEY);
        }

        /**
         * Scenariusz: wygenerowany kod grupy ma dokładnie 6 znaków (format #xxxxx)
         * i pokrywa się z wartością w bazie danych.
         */
        @Test
        @DisplayName("Tworzenie grupy: kod grupy ma 6 znaków i jest zapisany w bazie")
        void createGroup_validRequest_groupCodeIs6CharsAndPersistedCorrectly() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(jwt, GROUP_NAME);
            String returnedCode = fetchGroupCode(groupJwt);
            String dbCode = groupRepository.findByGroupName(GROUP_NAME).orElseThrow().getKod();
            assertThat(returnedCode).hasSize(6);
            assertThat(returnedCode).isEqualTo(dbCode);
        }

        /**
         * Scenariusz: brak nagłówka Authorization rzuca status 401.
         */
        @Test
        @DisplayName("Tworzenie grupy: brak nagłówka Authorization rzuca status 401")
        void createGroup_noAuthHeader_returns401() throws Exception {
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(GROUP_NAME);
            req.setCreatorKey(CREATOR_KEY);
            mockMvc.perform(post("/api/groups/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().is4xxClientError());
        }

        /**
         * Scenariusz: nieprawidłowy token (losowy ciąg) rzuca status 401.
         */
        @Test
        @DisplayName("Tworzenie grupy: nieprawidłowy token rzuca status 401")
        void createGroup_invalidToken_returns401() throws Exception {
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(GROUP_NAME);
            req.setCreatorKey(CREATOR_KEY);
            mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer to.nie.jest.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * Scenariusz: nazwa grupy za krótka (< 3 znaki) rzuca BadRequest 400 i żadna grupa nie trafia do bazy.
         */
        @Test
        @DisplayName("Tworzenie grupy: nazwa za krótka (< 3 znaki) daje BadRequest 400, brak zapisu w bazie")
        void createGroup_nameTooShort_returns400AndNothingSaved() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName("AB");
            req.setCreatorKey(CREATOR_KEY);
            mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            assertThat(groupRepository.findAll()).isEmpty();
        }

        /**
         * Scenariusz: nazwa grupy za długa (> 20 znaków) rzuca BadRequest -  400 i żadna grupa nie trafia do bazy.
         */
        @Test
        @DisplayName("Tworzenie grupy: nazwa za długa (> 20 znaków) daje BadRequest, brak zapisu w bazie")
        void createGroup_nameTooLong_returns400AndNothingSaved() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName("NazwaKtoraMaWiecejNiz20Z");
            req.setCreatorKey(CREATOR_KEY);
            mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
            assertThat(groupRepository.findAll()).isEmpty();
        }

        /**
         * Scenariusz: odpowiedź zawiera username twórcy zgodny z bazą danych.
         */
        @Test
        @DisplayName("Tworzenie grupy: odpowiedź zawiera username twórcy zgodny z bazą")
        void createGroup_validRequest_responseContainsCreatorUsername() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(GROUP_NAME);
            req.setCreatorKey(CREATOR_KEY);
            mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userCredentials.username").value(CREATOR_USERNAME));
        }

        /**
         * Scenariusz: pełny przepływ tworzenia — dane z odpowiedzi są spójne z bazą danych
         * (groupId, userId, kod grupy).
         */
        @Test
        @DisplayName("Tworzenie grupy: pełny przepływ — dane w odpowiedzi spójne z bazą danych")
        void createGroup_fullFlow_responseDataConsistentWithDatabase() throws Exception {
            String jwt = registerAndLogin(CREATOR_USERNAME);
            CreateGroupRequest req = new CreateGroupRequest();
            req.setGroupName(GROUP_NAME);
            req.setCreatorKey(CREATOR_KEY);
            MvcResult result = mockMvc.perform(post("/api/groups/create")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();
            flushAndClear();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            long responseUserId  = body.get("userCredentials").get("id").asLong();
            long responseGroupId = body.get("userCredentials").get("groupId").asLong();
            User dbCreator = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
            Group dbGroup   = groupRepository.findByGroupName(GROUP_NAME).orElseThrow();
            assertThat(responseUserId).isEqualTo(dbCreator.getId());
            assertThat(responseGroupId).isEqualTo(dbGroup.getId());
        }

        /**
         * Scenariusz: poprawny kod dołączenia daje status 200 (powodzenie), nowy JWT w odpowiedzi.
         */
        @Test
        @DisplayName("Dołączanie do grupy: poprawny kod daje 200 i nowy JWT")
        void joinGroup_validCode_returns200WithJwt() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jwt").isNotEmpty())
                    .andExpect(jsonPath("$.message").value("Dolaczono do grupy"));
        }

        /**
         * Scenariusz: po dołączeniu, użytkownik ma przypisaną grupę w bazie danych.
         */
        @Test
        @DisplayName("Dołączanie do grupy: user przypisany do grupy w bazie po dołączeniu")
        void joinGroup_validCode_memberAssignedToGroupInDatabase() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt   = createGroup(creatorJwt, GROUP_NAME);
            String code       = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
            flushAndClear();
            User member  = userRepository.findByUsername(MEMBER_USERNAME).orElseThrow();
            Group dbGroup = groupRepository.findByGroupName(GROUP_NAME).orElseThrow();
            assertThat(member.getGroup()).isNotNull();
            assertThat(member.getGroup().getId()).isEqualTo(dbGroup.getId());
        }

        /**
         * Scenariusz: po dołączeniu tworzony jest rekord GroupKey ze statusem PENDING.
         */
        @Test
        @DisplayName("Dołączanie do grupy: GroupKey nowego użytkownika ma status PENDING")
        void joinGroup_validCode_groupKeyCreatedWithPendingStatus() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
            flushAndClear();
            User member  = userRepository.findByUsername(MEMBER_USERNAME).orElseThrow();
            Group dbGroup = groupRepository.findByGroupName(GROUP_NAME).orElseThrow();
            Optional<GroupKey> gkOpt = groupKeyRepository
                    .findByGroupIdAndUserId(dbGroup.getId(), member.getId());
            assertThat(gkOpt).isPresent();
            assertThat(gkOpt.get().getStatus()).isEqualTo("PENDING");
        }

        /**
         * Scenariusz: klucz grupowy nowego użytkownika jest null przy tworzeniu (czeka na dostarczenie).
         */
        @Test
        @DisplayName("Dołączanie do grupy: encryptedGroupKey nowego użytkownika jest null (PENDING)")
        void joinGroup_validCode_groupKeyIsNullUntilDelivered() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
            flushAndClear();
            User member  = userRepository.findByUsername(MEMBER_USERNAME).orElseThrow();
            Group dbGroup = groupRepository.findByGroupName(GROUP_NAME).orElseThrow();
            GroupKey gk = groupKeyRepository
                    .findByGroupIdAndUserId(dbGroup.getId(), member.getId())
                    .orElseThrow();
            assertThat(gk.getEncryptedGroupKey()).isNull();
        }

        /**
         * Scenariusz: odpowiedź zawiera groupId zgodne z id grupy w bazie danych.
         */
        @Test
        @DisplayName("Dołączanie do grupy: userCredentials.groupId w odpowiedzi zgodne z bazą")
        void joinGroup_validCode_responseGroupIdMatchesDatabase() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode(code);
            MvcResult result = mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();
            flushAndClear();
            Long responseGroupId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("userCredentials").get("groupId").asLong();
            Long dbGroupId = groupRepository.findByGroupName(GROUP_NAME).orElseThrow().getId();
            assertThat(responseGroupId).isEqualTo(dbGroupId);
        }

        /**
         * Scenariusz: nieistniejący kod grupy rzuca wyjatek RuntimeException z odpowiednim komunikatem
         */
        @Test
        @DisplayName("Dołączanie do grupy: nieistniejący kod rzuca wyjatek RuntimeException (brak grupy w bazie)")
        void joinGroup_nonExistentCode_throwsServletException() throws Exception {
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode("#zzzzz");
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))));
            assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("Brak grupy");
        }

        /**
         * Scenariusz: kod za krótki (< 6 znaków) rzuca status 400 (BadRequest).
         */
        @Test
        @DisplayName("Dołączanie do grupy: kod za krótki (< 6 znaków) rzuca BadRequest 400")
        void joinGroup_codeTooShort_returns400() throws Exception {
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode("AB1");
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
            flushAndClear();
            assertThat(userRepository.findByUsername(MEMBER_USERNAME).orElseThrow().getGroup()).isNull();
        }

        /**
         * Scenariusz: kod za długi (> 6 znaków) rzuca status 400 (BadRequest).
         */
        @Test
        @DisplayName("Dołączanie do grupy: kod za długi (> 6 znaków) rzuca BadRequest 400")
        void joinGroup_codeTooLong_returns400() throws Exception {
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode("#abcdefgh");
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
            flushAndClear();
            assertThat(userRepository.findByUsername(MEMBER_USERNAME).orElseThrow().getGroup()).isNull();
        }

        /**
         * Scenariusz: brak nagłówka Authorization rzuca błąd 4xx.
         */
        @Test
        @DisplayName("Dołączanie do grupy: brak nagłówka Authorization daje blad 4xx")
        void joinGroup_noAuthHeader_returns4xx() throws Exception {
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode("#abcde");
            mockMvc.perform(post("/api/groups/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))).andExpect(status().is4xxClientError());
        }

        /**
         * Scenariusz: nieprawidłowy token rzuca status 401 (Unauthorized).
         */
        @Test
        @DisplayName("Dołączanie do grupy: nieprawidłowy token rzuca status 401")
        void joinGroup_invalidToken_returns401() throws Exception {
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode("#abcde");
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer to.nie.jest.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))).andExpect(status().isUnauthorized());
        }

        /**
         * Scenariusz: użytkownik już należący do grupy próbuje dołączyć ponownie rzuca status 409 - Conflict.
         */
        @Test
        @DisplayName("Dołączanie do grupy: user już w grupie rzuca status 409 Conflict")
        void joinGroup_userAlreadyInGroup_returns409() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt   = createGroup(creatorJwt, GROUP_NAME);
            String code       = fetchGroupCode(groupJwt);
            JoinGroupRequest req = new JoinGroupRequest();
            req.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + groupJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());
        }

        /**
         * Scenariusz: dwóch różnych użytkowników dołącza do tej samej grupy wówczas
         * w bazie danych obu ma to samo groupId.
         */
        @Test
        @DisplayName("Dołączanie do grupy: dwóch członków należy do tej samej grupy w bazie")
        void joinGroup_twoMembers_bothAssignedToSameGroupInDatabase() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String member1Jwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest req1 = new JoinGroupRequest();
            req1.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + member1Jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isOk());
            String member2Jwt = registerAndLogin("czlonek2");
            JoinGroupRequest req2 = new JoinGroupRequest();
            req2.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + member2Jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isOk());
            flushAndClear();

            Group dbGroup   = groupRepository.findByGroupName(GROUP_NAME).orElseThrow();
            User member1Db = userRepository.findByUsername(MEMBER_USERNAME).orElseThrow();
            User member2Db = userRepository.findByUsername("czlonek2").orElseThrow();
            assertThat(member1Db.getGroup().getId()).isEqualTo(dbGroup.getId());
            assertThat(member2Db.getGroup().getId()).isEqualTo(dbGroup.getId());
        }

        /**
         * Scenariusz: pełny przepływ — twórca tworzy grupę, członek dołącza
         * GET /api/groups/ zwraca oboje, dane spójne z bazą danych.
         */
        @Test
        @DisplayName("Pełny przepływ create oraz join: obaj użytkownicy widoczni w liście członków")
        void fullFlow_createThenJoin_bothUsersVisibleInGroupMembers() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest joinReq = new JoinGroupRequest();
            joinReq.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(joinReq)))
                    .andExpect(status().isOk());
            flushAndClear();
            mockMvc.perform(get("/api/groups/")
                            .header("Authorization", "Bearer " + groupJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andExpect(jsonPath("$.users[?(@.username == '" + CREATOR_USERNAME + "')]").exists())
                    .andExpect(jsonPath("$.users[?(@.username == '" + MEMBER_USERNAME + "')]").exists());
        }

        /**
         * Scenariusz: pełny przepływ — twórca i nowy member mają oba rekordy GroupKey
         * w bazie (ACTIVE dla twórcy, PENDING dla nowego).
         */
        @Test
        @DisplayName("Pełny przepływ create oraz join: rekordy GroupKey spójne z bazą (ACTIVE + PENDING)")
        void fullFlow_createThenJoin_groupKeysConsistentWithDatabase() throws Exception {
            String creatorJwt = registerAndLogin(CREATOR_USERNAME);
            String groupJwt = createGroup(creatorJwt, GROUP_NAME);
            String code = fetchGroupCode(groupJwt);
            String memberJwt = registerAndLogin(MEMBER_USERNAME);
            JoinGroupRequest joinReq = new JoinGroupRequest();
            joinReq.setCode(code);
            mockMvc.perform(post("/api/groups/join")
                            .header("Authorization", "Bearer " + memberJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(joinReq)))
                    .andExpect(status().isOk());
            flushAndClear();
            User creator = userRepository.findByUsername(CREATOR_USERNAME).orElseThrow();
            User member  = userRepository.findByUsername(MEMBER_USERNAME).orElseThrow();
            Group dbGroup = groupRepository.findByGroupName(GROUP_NAME).orElseThrow();
            GroupKey creatorKey = groupKeyRepository
                    .findByGroupIdAndUserId(dbGroup.getId(), creator.getId()).orElseThrow();
            GroupKey memberKey  = groupKeyRepository
                    .findByGroupIdAndUserId(dbGroup.getId(), member.getId()).orElseThrow();
            assertThat(creatorKey.getStatus()).isEqualTo("ACTIVE");
            assertThat(memberKey.getStatus()).isEqualTo("PENDING");
        }
}