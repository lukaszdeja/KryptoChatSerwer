package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.DTO.*;
import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.handler.WebSocketHandler;
import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import com.KryptoChat.serwer.repositories.GroupRepository;
import com.KryptoChat.serwer.services.GroupService;
import com.KryptoChat.serwer.services.JWTService;
import com.KryptoChat.serwer.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    /**
     * Skladowe klasy testowej
     * Zamockowane serwisy, repozytoria i handler
     * Testy potrzebuja tych mockow zeby byly testami jednostkowymi a nie integracyjnymi
     */
    @Mock private GroupService groupService;
    @Mock private UserService userService;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupKeyRepository groupKeyRepository;
    @Mock private WebSocketHandler webSocketHandler;
    @Mock private JWTService jwtService;

    @InjectMocks
    private GroupController groupController;

    /**
     * Wykorzystywane w testach mocki usera i grupy
     */
    private User mockUser;
    private Group mockGroup;

    /**
     * Stale wykorzystywane do testow
     */
    private final String VALID_HEADER = "Bearer valid.token.here";
    private final String VALID_TOKEN  = "valid.token.here";
    private final Long   USER_ID      = 1L;
    private final Long   GROUP_ID     = 10L;

    /**
     * Setup do kazdego testu jednostkowego - utworzenie mocków usera oraz grupy
     */
    @BeforeEach
    void setUp() {
        mockGroup = new Group();
        mockGroup.setId(GROUP_ID);
        mockUser = new User("testUser", "hashedPassword");
        mockUser.setId(USER_ID);
        mockUser.setGroup(mockGroup);
    }

    /**
     * Test sprawdzajacy odrzucenie zadania utworzenia grupy jesli header requesta jest nullem
     */
    @Test
    void createGroup_shouldReturn401_whenHeaderIsNull() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("Grupa");

        ResponseEntity<?> response = groupController.createGroup(null, request);

        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy odrzucenie zadania utworzenia grupy jezeli header requesta ma zly prefix
     */
    @Test
    void createGroup_shouldReturn401_whenHeaderHasNoBearerPrefix() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("Grupa");
        ResponseEntity<?> response = groupController.createGroup("InvalidHeader token", request);
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy odrzucenie żądania utworzenia grupy jezeli token jest nieprawidlowy
     */
    @Test
    void createGroup_shouldReturn401_whenTokenIsInvalid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("Grupa");
        ResponseEntity<?> response = groupController.createGroup(VALID_HEADER, request);
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzający czy żądanie utworzenia grupy o zbyt krotkiej nazwie zostanie odrzucone
     */
    @Test
    void createGroup_shouldReturn400_whenGroupNameIsTooShort() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("AB");
        ResponseEntity<?> response = groupController.createGroup(VALID_HEADER, request);
        assertEquals(400, response.getStatusCode().value());
    }

    /**
     * Test sprawdzający czy żądanie utworzenia grupy o zbyt dlugiej nazwie zostanie odrzucone
     */
    @Test
    void createGroup_shouldReturn400_whenGroupNameIsTooLong() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("NazwaKtoraJestZaDlugaAbyBylaPoprawna");
        ResponseEntity<?> response = groupController.createGroup(VALID_HEADER, request);
        assertEquals(400, response.getStatusCode().value());
    }

    /**
     * Test sprawdzający czy uda się utworzyć grupe dla poprawnego tokenu i nazwy
     */
    @Test
    void createGroup_shouldReturn200_whenRequestIsValid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtService.generateToken(mockUser)).thenReturn("new.jwt.token");
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupService.createGroup("MojaGrupa", mockUser, "creatorKey")).thenReturn(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(mockGroup));
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("MojaGrupa");
        request.setCreatorKey("creatorKey");
        ResponseEntity<?> response = groupController.createGroup(VALID_HEADER, request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    /**
     * Test sprawdzający czy metoda mocka serwisu faktycznie jest wywolywana przy tworzeniu grupy
     */
    @Test
    void createGroup_shouldCallGroupServiceCreate_whenRequestIsValid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtService.generateToken(mockUser)).thenReturn("new.jwt.token");
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupService.createGroup("MojaGrupa", mockUser, "creatorKey")).thenReturn(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(mockGroup));
        CreateGroupRequest request = new CreateGroupRequest();
        request.setGroupName("MojaGrupa");
        request.setCreatorKey("creatorKey");
        groupController.createGroup(VALID_HEADER, request);
        verify(groupService).createGroup("MojaGrupa", mockUser, "creatorKey");
    }

    /**
     * Test sprawdzający czy żądanie dołączenia do grupy zostanie odrzucone jeśli nagłowek jest nullem
     */
    @Test
    void joinGroup_shouldReturn401_whenHeaderIsNull() {
        ResponseEntity<?> response = groupController.joinGroup(null, new JoinGroupRequest());

        assertEquals(401, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy żądanie dołączenia do grupy zostanie odrzucone jeśli token jest nieprawidlowy
     */
    @Test
    void joinGroup_shouldReturn401_whenTokenIsInvalid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);
        JoinGroupRequest request = new JoinGroupRequest();
        request.setCode("#abcd1");
        ResponseEntity<?> response = groupController.joinGroup(VALID_HEADER, request);
        assertEquals(401, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy żądanie dołączenia do grupy zostanie odrzucone jeśli kod jest nullem
     */
    @Test
    void joinGroup_shouldReturn400_whenCodeIsNull() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        JoinGroupRequest request = new JoinGroupRequest();
        request.setCode(null);
        ResponseEntity<?> response = groupController.joinGroup(VALID_HEADER, request);
        assertEquals(400, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy żądanie dołączenia do grupy zostanie odrzucone jeśli kod ma złą długosc
     */
    @Test
    void joinGroup_shouldReturn400_whenCodeHasWrongLength() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        JoinGroupRequest request = new JoinGroupRequest();
        request.setCode("#abc");
        ResponseEntity<?> response = groupController.joinGroup(VALID_HEADER, request);
        assertEquals(400, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy żądanie dołączenia do grupy zostanie odrzucone jeśli mock usera ma juz przypisany mock grupy
     */
    @Test
    void joinGroup_shouldReturn409_whenUserAlreadyHasGroup() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        JoinGroupRequest request = new JoinGroupRequest();
        request.setCode("#abc12");
        ResponseEntity<?> response = groupController.joinGroup(VALID_HEADER, request);
        assertEquals(409, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy żądanie dołączenia do grupy zostanie odrzucone jeśli mock grupy nie istnieje
     */
    @Test
    void joinGroup_shouldReturn400_whenGroupDoesNotExist() {
        User userWithoutGroup = new User("noGroup", "pass");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(userWithoutGroup);
        when(groupService.joinGroup("#abc12", userWithoutGroup)).thenReturn(null);
        JoinGroupRequest request = new JoinGroupRequest();
        request.setCode("#abc12");
        ResponseEntity<?> response = groupController.joinGroup(VALID_HEADER, request);
        assertEquals(400, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy poprawnie uda się dolaczyc do grupy dla prawidlowego tokenu i kodu - kod 200 otrzymany
     */
    @Test
    void joinGroup_shouldReturn200_whenJoinIsSuccessful() {
        User userWithoutGroup = new User("noGroup", "pass");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(userWithoutGroup);
        when(groupService.joinGroup("#abc12", userWithoutGroup))
                .thenAnswer(invocation -> {
                    userWithoutGroup.setGroup(mockGroup);
                    return GROUP_ID;
                });

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(mockGroup));
        when(jwtService.generateToken(userWithoutGroup)).thenReturn("new.jwt.token");
        JoinGroupRequest request = new JoinGroupRequest();
        request.setCode("#abc12");
        ResponseEntity<?> response = groupController.joinGroup(VALID_HEADER, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof GroupResponse);
        GroupResponse body = (GroupResponse) response.getBody();
        assertEquals("Dolaczono do grupy", body.getMessage());
        verify(groupService).joinGroup("#abc12", userWithoutGroup);
        verify(groupRepository).findById(GROUP_ID);
        verify(jwtService).generateToken(userWithoutGroup);
    }


    /**
     * Test sprawdzający czy żądanie zaczytania czlonkow grupy bedzie odrzucone jesli naglowek jest nullem
     */
    @Test
    void getGroup_shouldReturn401_whenHeaderIsNull() {
        ResponseEntity<?> response = groupController.getGroup(null);
        assertEquals(401, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy zaczyatnie czlonkow grupy zostanie odrzucone jeśli token jest nieprawidlowy
     */
    @Test
    void getGroup_shouldReturn401_whenTokenIsInvalid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);
        ResponseEntity<?> response = groupController.getGroup(VALID_HEADER);
        assertEquals(401, response.getStatusCode().value());
    }


    /**
     * Test sprawdzający czy zaczyatnie czlonkow grupy przejdzie poprawnie i zwroci 200 dla poprawnego requesta
     */
    @Test
    void getGroup_shouldReturn200_whenRequestIsValid() {
        mockGroup.setUsers(List.of(mockUser));
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(mockGroup));
        ResponseEntity<?> response = groupController.getGroup(VALID_HEADER);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    /**
     * Test sprawdzajacy utworzony obiekt odpowiedzi jest poprawny
     */
    @Test
    void getGroup_shouldReturnGroupDetails_whenRequestIsValid() {
        mockGroup.setUsers(List.of(mockUser));
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(mockGroup));
        ResponseEntity<GroupDetailsResponse> response = groupController.getGroup(VALID_HEADER);
        GroupDetailsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(GROUP_ID, body.getGroupId());
    }

    /**
     * Test sprawdzajacy czy metoda dostarczenia klucza odrzuci przeslane zadanie jesli header jest nullem
     */
    @Test
    void deliverKey_shouldReturn401_whenHeaderIsNull() {
        ResponseEntity<Void> response = groupController.deliverKey(null, new DeliverKeyRequest());
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy przeslanie klucza zostanie odrzucone jesli token jest nieprawidlowy
     */
    @Test
    void deliverKey_shouldReturn401_whenTokenIsInvalid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);
        ResponseEntity<Void> response = groupController.deliverKey(VALID_HEADER, new DeliverKeyRequest());
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy zmiana statusu klucza ktory juz jest aktywny zostanie odrzucona
     */
    @Test
    void deliverKey_shouldReturn400_whenGroupKeyStatusIsNotPending() {
        GroupKey gk = new GroupKey();
        gk.setStatus("ACTIVE");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupKeyRepository.findByGroupIdAndUserId(GROUP_ID, 99L)).thenReturn(Optional.of(gk));
        DeliverKeyRequest request = new DeliverKeyRequest();
        request.setTargetUserId(99L);
        request.setEncryptedKey("encryptedKeyData");
        ResponseEntity<Void> response = groupController.deliverKey(VALID_HEADER, request);
        assertEquals(400, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy uda sie zmienic status klucza z PENDING na ACTIVE jesli request i token byly prawidlowe
     */
    @Test
    void deliverKey_shouldReturn200_whenStatusIsPending() {
        GroupKey gk = new GroupKey();
        gk.setStatus("PENDING");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupKeyRepository.findByGroupIdAndUserId(GROUP_ID, 99L)).thenReturn(Optional.of(gk));
        DeliverKeyRequest request = new DeliverKeyRequest();
        request.setTargetUserId(99L);
        request.setEncryptedKey("encryptedKeyData");
        ResponseEntity<Void> response = groupController.deliverKey(VALID_HEADER, request);
        assertEquals(200, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy powiadomienie klienta o dostarczeniu klucza zadziala poprawnie
     */
    @Test
    void deliverKey_shouldNotifyTargetUser_whenKeyDelivered() {
        GroupKey gk = new GroupKey();
        gk.setStatus("PENDING");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupKeyRepository.findByGroupIdAndUserId(GROUP_ID, 99L)).thenReturn(Optional.of(gk));
        DeliverKeyRequest request = new DeliverKeyRequest();
        request.setTargetUserId(99L);
        request.setEncryptedKey("encryptedKeyData");
        groupController.deliverKey(VALID_HEADER, request);
        verify(webSocketHandler).notifyKeyReady(99L);
    }

    /**
     * Test sprawdzajacy czy request pobrania klucza dla naglowka ktory jest nullem zostanie odrzucony
     */
    @Test
    void getMyKey_shouldReturn401_whenHeaderIsNull() {
        ResponseEntity<String> response = groupController.getMyKey(null);
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy request pobrania klucza dla nieprawidlowego tokenu zostanie odrzucony
     */
    @Test
    void getMyKey_shouldReturn401_whenTokenIsInvalid() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);
        ResponseEntity<String> response = groupController.getMyKey(VALID_HEADER);
        assertEquals(401, response.getStatusCode().value());
    }

    /**
     * Test sprawdzajacy czy request pobrania tokenu, ktory jest nieaktywny, nie moze zostac w tym momencie przyjety
     */
    @Test
    void getMyKey_shouldReturn202WithPending_whenKeyIsNotActive() {
        GroupKey gk = new GroupKey();
        gk.setStatus("PENDING");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupKeyRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(gk));
        ResponseEntity<String> response = groupController.getMyKey(VALID_HEADER);
        assertEquals(202, response.getStatusCode().value());
        assertEquals("PENDING", response.getBody());
    }

    /**
     * Test sprawdzajacy czy uda sie pobrac aktywny klucz
     */
    @Test
    void getMyKey_shouldReturn200WithKey_whenKeyIsActive() {
        GroupKey gk = new GroupKey();
        gk.setStatus("ACTIVE");
        gk.setEncryptedGroupKey("super-secret-encrypted-key");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.authentification(USER_ID)).thenReturn(mockUser);
        when(groupKeyRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(gk));
        ResponseEntity<String> response = groupController.getMyKey(VALID_HEADER);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("super-secret-encrypted-key", response.getBody());
    }
}