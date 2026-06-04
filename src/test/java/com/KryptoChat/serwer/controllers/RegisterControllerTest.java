package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.DTO.RegisterRequest;
import com.KryptoChat.serwer.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    private static final String VALID_PASSWORD = "Test1234!";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private RegisterController registerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registerController).build();
        objectMapper = new ObjectMapper();
    }

    private RegisterRequest buildValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testUser");
        request.setPassword(VALID_PASSWORD);
        request.setPublicKey("publicKeyValue");
        request.setEncryptedPrivateKey("encryptedPrivateKeyValue");
        return request;
    }

    @Test
    @DisplayName("Rejestracja poprawnego użytkownika zwraca 200 OK z komunikatem")
    void register_ValidRequest_Returns200WithMessage() throws Exception {
        doNothing().when(userService).register(any(), any(), any(), any());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string("Uzytkownik sie zarejestrowal"));
    }

    @Test
    @DisplayName("Rejestracja wywołuje userService.register z poprawnymi argumentami")
    void register_ValidRequest_CallsUserServiceWithCorrectArguments() throws Exception {
        doNothing().when(userService).register(any(), any(), any(), any());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword(VALID_PASSWORD);
        request.setPublicKey("rsaPublicKey");
        request.setEncryptedPrivateKey("encPrivKey");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService, times(1)).register(
                "alice",
                VALID_PASSWORD,
                "rsaPublicKey",
                "encPrivKey"
        );
    }

    @Test
    @DisplayName("userService.register jest wywoływany dokładnie raz")
    void register_ValidRequest_UserServiceCalledExactlyOnce() throws Exception {
        doNothing().when(userService).register(any(), any(), any(), any());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk());

        verify(userService, times(1)).register(any(), any(), any(), any());
        verifyNoMoreInteractions(userService);
    }

    @Test
    @DisplayName("Wyjątek z userService propaguje się poza kontroler")
    void register_UserServiceThrowsException_PropagatesError() {
        doThrow(new RuntimeException("Użytkownik już istnieje"))
                .when(userService).register(any(), any(), any(), any());

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
        );

        verify(userService, times(1)).register(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Żądanie bez body zwraca 400 Bad Request")
    void register_MissingBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Hasło za krótkie (poniżej 8 znaków) zwraca 400 Bad Request")
    void register_PasswordTooShort_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testUser");
        request.setPassword("Ab1!");
        request.setPublicKey("pubKey");
        request.setEncryptedPrivateKey("encKey");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Hasło bez znaku specjalnego zwraca 400 Bad Request")
    void register_PasswordWithoutSpecialChar_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testUser");
        request.setPassword("TestPassword1");
        request.setPublicKey("pubKey");
        request.setEncryptedPrivateKey("encKey");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Żądanie z nieprawidłowym Content-Type zwraca 415")
    void register_WrongContentType_Returns415() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("someText"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Odpowiedź ma Content-Type text/plain")
    void register_ResponseHasExpectedContentType() throws Exception {
        doNothing().when(userService).register(any(), any(), any(), any());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }
}