package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.DTO.LoginResponse;
import com.KryptoChat.serwer.DTO.RegisterRequest;
import com.KryptoChat.serwer.DTO.UserCredentials;
import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.UserService;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    /**
     * Mocki wykorzystywanych serwisów
     */
    @Mock
    private UserService userService;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private LoginController loginController;

    /**
     * Obiekty wykorzystywane w testach
     */
    private User user;
    private Group group;

    /**
     * Stale wykorzystywane w testach
     */
    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 100L;
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "Password123!";
    private static final String TOKEN = "jwt.token";
    private static final String AUTH_HEADER = "Bearer " + TOKEN;

    /**
     * Setup stanu obiektow user i group przed kazdym testem
     */
    @BeforeEach
    void setUp() {
        user = new User(USERNAME, PASSWORD);
        user.setId(USER_ID);
        user.setPublicKey("public-key");
        user.setEncryptedPrivateKey("encrypted-private-key");
        group = new Group();
        group.setId(GROUP_ID);
    }

    /**
     * Test sprawdzajacy czy serwer zwraca odpowiedz ze statusem 200 (sukces) jesli dane logowania sa poprawne
     * @throws Exception
     */
    @Test
    void login_shouldReturn200_whenCredentialsAreValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);
        when(userService.login(USERNAME, PASSWORD)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(TOKEN);
        ResponseEntity<LoginResponse> response = loginController.login(request);
        assertEquals(200, response.getStatusCode().value());
        LoginResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(TOKEN, body.getJwt());
        assertEquals("Zalogowano", body.getMessage());
        assertEquals(USER_ID, body.getUserCredentials().getId());
        assertEquals(USERNAME, body.getUserCredentials().getUsername());
        assertNull(body.getUserCredentials().getGroupId());
    }

    /**
     * Test sprawdzajacy czy dla usera w grupie po zalogowaniu zostanie zwrocone Id grupy
     * @throws Exception
     */
    @Test
    void login_shouldReturnGroupId_whenUserBelongsToGroup() throws Exception {
        user.setGroup(group);
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);
        when(userService.login(USERNAME, PASSWORD)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(TOKEN);
        ResponseEntity<LoginResponse> response = loginController.login(request);
        LoginResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(GROUP_ID, body.getUserCredentials().getGroupId());
    }

    /**
     * Test sprawdzajacy czy jesli username bedzie nullem to czy bedzie badRequest w odpowiedzi
     */
    @Test
    void login_shouldReturnBadRequest_whenUsernameIsNull() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(null);
        request.setPassword(PASSWORD);
        ResponseEntity<LoginResponse> response = loginController.login(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Login i haslo sa wymagane",
                response.getBody().getMessage()
        );
    }

    /**
     * Test sprawdzajacy czy dla hasla bedacego nullem w odpowiedzi zwrocony bedzie bad request
     */
    @Test
    void login_shouldReturnBadRequest_whenPasswordIsNull() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setPassword(null);
        ResponseEntity<LoginResponse> response = loginController.login(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Login i haslo sa wymagane",
                response.getBody().getMessage()
        );
    }

    /**
     * Test sprawdzajacy czy jesli header naglowka jest nullem (chodzi o endpoint /me)
     * To czy wowczas zwrocony zostanie blad 401
     */
    @Test
    void me_shouldReturn401_whenHeaderIsNull() {
        ResponseEntity<UserCredentials> response = loginController.me(null);
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy jesli w endpoincie /me header ma zly format
     * Czy zostanie wowczas zwrocony status 401
     */
    @Test
    void me_shouldReturn401_whenHeaderHasWrongFormat() {
        ResponseEntity<UserCredentials> response = loginController.me("wrong-header");
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy jezeli token jest nieprawidlowy to czy serwer zwroci odpowiedz 401
     */
    @Test
    void me_shouldReturn401_whenTokenIsInvalid() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(false);
        ResponseEntity<UserCredentials> response = loginController.me(AUTH_HEADER);
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy jezeli token jest prawidlowy to czy /me zwroci poprawne user credentials
     * oraz status 200
     */
    @Test
    void me_shouldReturn200_whenTokenIsValid() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(user);
        ResponseEntity<UserCredentials> response = loginController.me(AUTH_HEADER);
        assertEquals(200, response.getStatusCode().value());
        UserCredentials body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID, body.getId());
        assertEquals(USERNAME, body.getUsername());
        assertNull(body.getGroupId());
    }

    /**
     * Podobnie test tego czy dla usera ktory jest w grupie zostanie zwrocone id grupy
     * tylko ze dla endpointu /me a nie /login
     */
    @Test
    void me_shouldReturnGroupId_whenUserHasGroup() {
        user.setGroup(group);
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(user);
        ResponseEntity<UserCredentials> response = loginController.me(AUTH_HEADER);
        UserCredentials body = response.getBody();
        assertNotNull(body);
        assertEquals(GROUP_ID, body.getGroupId());
    }
}