package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.DTO.MessageList;
import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.services.ChatService;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_VALID = "Bearer " + VALID_TOKEN;
    private static final Long USER_ID = 1L;
    private static final Long GROUP_ID = 10L;

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @Mock
    private UserService userService;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
    }

    private User buildUserWithGroup() {
        Group group = new Group();
        group.setId(GROUP_ID);
        User user = new User();
        user.setGroup(group);
        return user;
    }

    @Test
    @DisplayName("Poprawny token zwraca 200 OK z listą wiadomości")
    void loadMessages_ValidToken_Returns200WithMessages() throws Exception {
        MessageList messageList = new MessageList();
        User user = buildUserWithGroup();

        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(user);
        when(chatService.loadMessages(GROUP_ID)).thenReturn(messageList);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", BEARER_VALID))
                .andExpect(status().isOk());

        verify(chatService, times(1)).loadMessages(GROUP_ID);
    }

    @Test
    @DisplayName("Nagłówek bez prefiksu 'Bearer ' zwraca 401")
    void loadMessages_HeaderWithoutBearerPrefix_Returns401() throws Exception {
        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtService, userService, chatService);
    }

    @Test
    @DisplayName("Nieprawidłowy token zwraca 401")
    void loadMessages_InvalidToken_Returns401() throws Exception {
        when(jwtService.isTokenValid("invalid.token")).thenReturn(false);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());

        verify(jwtService, times(1)).isTokenValid("invalid.token");
        verifyNoInteractions(userService, chatService);
    }

    @Test
    @DisplayName("Użytkownik bez grupy zwraca 401")
    void loadMessages_UserWithoutGroup_Returns401() throws Exception {
        User userWithoutGroup = new User();
        Group group = new Group();
        // group.getId() zwróci null — brak przypisanej grupy
        userWithoutGroup.setGroup(group);

        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(userWithoutGroup);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", BEARER_VALID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(chatService);
    }

    @Test
    @DisplayName("chatService.loadMessages jest wywoływany z poprawnym groupId")
    void loadMessages_ValidToken_CallsChatServiceWithCorrectGroupId() throws Exception {
        User user = buildUserWithGroup();

        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(user);
        when(chatService.loadMessages(GROUP_ID)).thenReturn(new MessageList());

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", BEARER_VALID))
                .andExpect(status().isOk());

        verify(chatService, times(1)).loadMessages(GROUP_ID);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    @DisplayName("jwtService.extractUserId jest wywoływany po poprawnej walidacji tokenu")
    void loadMessages_ValidToken_ExtractsUserIdFromToken() throws Exception {
        User user = buildUserWithGroup();

        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(user);
        when(chatService.loadMessages(GROUP_ID)).thenReturn(new MessageList());

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", BEARER_VALID))
                .andExpect(status().isOk());

        verify(jwtService, times(1)).extractUserId(VALID_TOKEN);
    }

    @Test
    @DisplayName("userService.authentification nie jest wywoływany dla nieprawidłowego tokenu")
    void loadMessages_InvalidToken_DoesNotCallAuthentification() throws Exception {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);

        mockMvc.perform(get("/api/messages/")
                        .header("Authorization", BEARER_VALID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}