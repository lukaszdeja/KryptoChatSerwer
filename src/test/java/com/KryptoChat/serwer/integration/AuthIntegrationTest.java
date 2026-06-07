package com.KryptoChat.serwer.integration;

import com.KryptoChat.serwer.DTO.RegisterRequest;
import com.KryptoChat.serwer.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.coyote.BadRequestException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testy integracyjne logowania, rejestracji i autentyfikacji
 * UWAGA - ta klasa korzysta z JWTService, który używa zmiennej środowiskowej JWT_SECRET
 * Aby testy przeszły poprawnie, konieczne jest ustawienie zmiennej środowiskowej JWT_SECRET jako ciag znakow nie krotszy niz 32 znaki
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
class AuthIntegrationTest {
    /**
     * Zamockowane wykorzystywane obiekty
     */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Stale wykorzystywane w testach
     */
    private static final String USERNAME  = "testUser";
    private static final String PASSWORD  = "Test1234!";
    private static final String PUB_KEY   = "publicKeyRSA";
    private static final String ENC_KEY   = "encryptedPrivateKey";

    /**
     * Metoda pomocnicza budująca DTO Register Requesta
     * @return RegisterRequest
     */
    private RegisterRequest buildValidRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(USERNAME);
        req.setPassword(PASSWORD);
        req.setPublicKey(PUB_KEY);
        req.setEncryptedPrivateKey(ENC_KEY);
        return req;
    }

    /**
     * Metoda pomocnicza budujaca DTO Register Request z nazwa uzytkownika
     * @param username
     * @return RegisterRequest
     */
    private RegisterRequest buildValidRequest(String username) {
        RegisterRequest req = buildValidRequest();
        req.setUsername(username);
        return req;
    }

    /**
     * Metoda pomocnicza rejestrujaca usera i zwracajaca JWT z odpoweidzi
     */
    private String registerAndLogin(String username) throws Exception {
        RegisterRequest req = buildValidRequest(username);
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String body = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("jwt").asText();
    }


    /**
     * Test sprawdzajacy czy dla poprawnych danych uzytkownik zostaje zapisany w bazie danych
     * @throws Exception
     */
    @Test
    @DisplayName("Rejestracja: poprawne dane → 200 i user zapisany w bazie")
    void register_validRequest_returns200AndPersistsUser() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string("Uzytkownik sie zarejestrowal"));

        assertThat(userRepository.existsByUsername(USERNAME)).isTrue();
    }

    /**
     * Test sprawdzajacy czy zapisane w bazie danych jest odpowiednio zahashowane
     * @throws Exception
     */
    @Test
    @DisplayName("Rejestracja: hasło jest hashowane — plain-text nie jest zapisany w bazie")
    void register_passwordIsHashed_notStoredAsPlaintext() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        String storedPassword = userRepository.findByUsername(USERNAME)
                .orElseThrow().getPassword();

        assertThat(storedPassword).isNotEqualTo(PASSWORD);
        assertThat(storedPassword).startsWith("$2a$"); // BCrypt prefix
    }

    /**
     * Test sprawdzajacy czy klucze zostają zapisane w bazie danych
     * @throws Exception
     */
    @Test
    @DisplayName("Rejestracja: publicKey i encryptedPrivateKey są zapisane w bazie")
    void register_keysArePersisted() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        var user = userRepository.findByUsername(USERNAME).orElseThrow();
        assertThat(user.getPublicKey()).isEqualTo(PUB_KEY);
        assertThat(user.getEncryptedPrivateKey()).isEqualTo(ENC_KEY);
    }

    /**
     * Test sprawdzający czy po próbie rejestracji usera z tymi samymi danymi, zostanie rzucony wyjątek umozliwiajacy duplikacje w bazie danych
     * @throws Exception
     */
    @Test
    @DisplayName("Rejestracja: duplikat username → błąd ponowna próba rzuci wyjątek")
    void register_duplicateUsername_returnsErrorAndDoesNotDuplicate() throws Exception {

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
        )
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request processing failed");
    }

    /**
     * Test sprawdzający czy jeżeli hasło jest za krótkie, to czy w bazie danych nic nie zostało zapisane
     * (Test prawidłowego odrzucania requesta)
     * @throws Exception
     */
    @Test
    @DisplayName("Rejestracja: za krótkie hasło → 400, nic nie zapisano")
    void register_passwordTooShort_returns400AndNoUserSaved() throws Exception {
        RegisterRequest req = buildValidRequest();
        req.setPassword("Ab1!");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.existsByUsername(USERNAME)).isFalse();
    }

    /**
     * Test sprawdzający czy request z nieprawidłowym formatem hasła zostanie odrzucony (nic w bazie sie nie zapisuje)
     * @throws Exception
     */
    @Test
    @DisplayName("Rejestracja: hasło bez znaku specjalnego → 400, nic nie zapisano")
    void register_passwordWithoutSpecialChar_returns400AndNoUserSaved() throws Exception {
        RegisterRequest req = buildValidRequest();
        req.setPassword("TestPassword1");
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.existsByUsername(USERNAME)).isFalse();
    }


    /**
     * Test sprawdzający czy podanie poprawnych danych logowania zwraca token i wlasciwy komunikat
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: poprawne dane → 200, JWT w odpowiedzi, komunikat 'Zalogowano'")
    void login_validCredentials_returns200WithJwt() throws Exception {
        // najpierw rejestracja przez serwis — żeby mieć usera w bazie
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zalogowano"))
                .andExpect(jsonPath("$.jwt").isNotEmpty())
                .andExpect(jsonPath("$.userCredentials.username").value(USERNAME));
    }

    /**
     * Test sprawdzający czy w odpowiedzi logowania są klucze pobrane z bazy danych
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: odpowiedź zawiera publicKey i encryptedPrivateKey")
    void login_validCredentials_returnsKeys() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value(PUB_KEY))
                .andExpect(jsonPath("$.encryptedPrivateKey").value(ENC_KEY));
    }

    /**
     * Test sprawdzający czy po poprawnym logowaniu, z bazy zostanie pobrane i zwrócone prawidłowe user credentials
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: odpowiedź zawiera poprawne userId (zgodne z bazą)")
    void login_validCredentials_returnsCorrectUserId() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        Long dbUserId = userRepository.findByUsername(USERNAME).orElseThrow().getId();

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCredentials.id").value(dbUserId));
    }

    /**
     * Test sprawdzający czy po logowaniu dla usera który nie jest w grupie, z bazy zostanie odpowiednio zaczytane
     * user id = null
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: nowy user (bez grupy) → groupId w odpowiedzi jest null")
    void login_userWithoutGroup_groupIdIsNull() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCredentials.groupId").doesNotExist());
    }

    /**
     * Test sprawdzający czy błędne hasło w requescie oznacza błąd serwera (wyjątek) zwrócony
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: błędne hasło → błąd serwera")
    void login_wrongPassword_returnsError() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        RegisterRequest wrongPass = buildValidRequest();
        wrongPass.setPassword("ZleHaslo1!");

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass))))
                .hasRootCauseMessage("Błędne hasło");
    }

    /**
     * Test sprawdzający czy po logowaniu jako użytkownik który nie istnieje, rzucony zostanie wyjątek
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: nieistniejący user → błąd serwera")
    void login_unknownUser_returnsError() throws Exception {
        RegisterRequest req = buildValidRequest();
        req.setUsername("nieistniejacy");

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .hasRootCauseMessage("Nie ma takiego użytkownika");
    }

    /**
     * Test sprawdzający czy pusty request (null) da odpowiedz BadRequesst
     * @throws Exception
     */
    @Test
    @DisplayName("Logowanie: null username → 400 BadRequest")
    void login_nullUsername_returns400() throws Exception {
        RegisterRequest req = buildValidRequest();
        req.setUsername(null);
        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))));
    }

    /**
     * Test sprawdzający token JWT w autentyfikacji
     * Jeżeli token zostanie uznany za Valid to wówczas
     * Serwer zwróci w odpowiedzi prawidłowe dane (user credentials)
     * @throws Exception
     */
    @Test
    @DisplayName("/me: poprawny JWT → 200, poprawne dane usera")
    void me_validToken_returns200WithUserData() throws Exception {
        String jwt = registerAndLogin(USERNAME);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.id").isNumber());
    }

    /**
     * Test sprawdzający czy dla usera który nie ma grupy, autentyfikacja prawidlowo pobierze z bazy i zwroci groupId = null
     * @throws Exception
     */
    @Test
    @DisplayName("/me: poprawny JWT, user bez grupy → groupId jest null")
    void me_validToken_userWithoutGroup_groupIdIsNull() throws Exception {
        String jwt = registerAndLogin(USERNAME);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").doesNotExist());
    }

    /**
     * Test sprawdzajacy czy dla usera ktory nie przeslal naglowka w autentyfikacji
     * czy serwer prawidlowo odrzuci (BadRequest)
     * @throws Exception
     */
    @Test
    @DisplayName("/me: brak nagłówka Authorization → BadRequest")
    void me_noAuthHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test sprawdzajacy czy jezeli naglowek ma zla tresc to czy w odpowiedzi zwrocony zostanie status Unauthorized
     * (401)
     * @throws Exception
     */
    @Test
    @DisplayName("/me: nagłówek bez prefiksu 'Bearer ' → 401")
    void me_headerWithoutBearerPrefix_returns401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "nieberarer cos"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test sprawdzający czy nieprawidłowy token będzie odrzucany - 401
     * @throws Exception
     */
    @Test
    @DisplayName("/me: losowy string jako token → 401")
    void me_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer to.nie.jest.prawidlowy.token"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test sprawdzający czy dane z tokenu zgadzają się z danymi w bazie danych
     * @throws Exception
     */
    @Test
    @DisplayName("/me: JWT z poprawną rejestracją → userId w /me zgadza się z bazą danych")
    void me_validToken_userIdMatchesDatabase() throws Exception {
        String jwt = registerAndLogin(USERNAME);
        Long dbId = userRepository.findByUsername(USERNAME).orElseThrow().getId();

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dbId));
    }

    /**
     * Test integracyjny całego flow funkcjonalności rejestracji i logowania
     * Na początek ma miejsce rejestracja (utworzenie konta)
     * W dalszym ciągu wykonywane jest logowanie dla danych obiektu wcześniej utworzonego
     * Sprawdzane jest czy logowanie zwróci prawidłową odpowiedz
     * Oraz czy zwrócony zostanie token jwt z danymi zgodnymi z tymi w bazie danych
     * @throws Exception
     */
    @Test
    @DisplayName("Pełny przepływ: rejestracja → logowanie → /me zwraca spójne dane")
    void fullFlow_register_login_me_consistentData() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andReturn();

        String jwt = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        ).get("jwt").asText();

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));
    }
}