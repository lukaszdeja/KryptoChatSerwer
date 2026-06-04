package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.*;
import com.KryptoChat.serwer.entities.*;
import com.KryptoChat.serwer.repositories.GroupRepository;
import com.KryptoChat.serwer.handler.WebSocketHandler;

import java.util.List;

/**
 * Klasa obsługująca backendowy kontroler REST API dla widoku grup na kliencie, obsługuje żądania REST
 */

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final GroupRepository groupRepository;
    private final GroupKeyRepository groupKeyRepository;
    private final WebSocketHandler webSocketHandler;
    private final JWTService jwtService;

    /**
     * Konstruktor inicjujący pola klasy
     * @param groupService
     * @param userService
     * @param groupRepository
     */
    public GroupController(GroupService groupService, UserService userService, GroupRepository groupRepository, GroupKeyRepository gkr, WebSocketHandler wsh, JWTService jwtService) {
        this.groupService = groupService;
        this.userService = userService;
        this.groupRepository = groupRepository;
        this.groupKeyRepository = gkr;
        this.webSocketHandler = wsh;
        this.jwtService = jwtService;
    }

    /**
     * Metoda odpowiedzialna za utworzenie grupy w bazie danych i przypisanie idGrupy tworzącemu ją użytkownikowi
     * Waliduje token jwt przesłany w headerze, wykonuje operacje na bazie danych i zwraca odpowiedź serwera - nowy token wraz z DTO grupy
     * @param header
     * @param request
     * @return ResponseEntity<GroupResponse>
     */
    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestHeader("Authorization") String header, @RequestBody CreateGroupRequest request) {

        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        if (request.getGroupName().length() > 20 || request.getGroupName().length() < 3 || request.getGroupName() == null) {
            return ResponseEntity.badRequest().body("Nazwa grupy musi miec minimum 3 znaki i maksimum 20");
        }
        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);

        Long groupId = groupService.createGroup(request.getGroupName(), user, request.getCreatorKey());
        String newToken = null;
        String message = "Nie udalo sie utworzyc grupy";
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        if (group != null) {
            newToken = jwtService.generateToken(user);
            message = "Uwtorzono grupe";
        }

        return ResponseEntity.ok(new GroupResponse(newToken, new UserCredentials(user.getId(), user.getUsername(), user.getGroup().getId()), message));
    }

    /**
     * Metoda obsługująca procedurę dołączania do grupy po kodzie dołączenia, waliduje token jwt, weryfikuje kod
     * Jeśli jest poprawny przypisuje id grupy użytkownikowi w bazie oraz zwraca informacje o dołączeniu na klienta
     * @param header
     * @param request
     * @return
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinGroup(@RequestHeader("Authorization") String header, @RequestBody JoinGroupRequest request) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        if (request.getCode() == null || request.getCode().length() != 6) {
            return ResponseEntity.badRequest().body("Kod dolaczenia jest zlej dlugosci");
        }
        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);
        if (user.getGroup() != null) {
            if (user.getGroup().getId() != null) {
                return ResponseEntity.status(409).build();
            }
            return ResponseEntity.status(409).build();
        }

        Long groupId = groupService.joinGroup(request.getCode(), user);
        if (groupId == null) {
            return ResponseEntity.badRequest().body("Grupa nie istnieje");
        }
        String newToken = null;
        String message = "";
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));
        if (group != null) {
            newToken = jwtService.generateToken(user);
            message = "Dolaczono do grupy";
        }

        return ResponseEntity.ok(new GroupResponse(newToken, new UserCredentials(user.getId(), user.getUsername(), user.getGroup().getId()), message));
    }

    /**
     * Metoda obsługująca zapytania typu GET, pobiera członków grupy, do której należy użytkownik, jeśli należy do jakiejs
     * Następnie zwraca listę użytkowników z tej grupy w formie List. Waliduje jwt
     * @param header
     * @return
     */
    @GetMapping("/")
    public ResponseEntity<GroupDetailsResponse> getGroup(@RequestHeader("Authorization")  String header) {

        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);


        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);

        Long groupId = user.getGroup().getId();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        List<UserResponse> users = group.getUsers()
                .stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername()))
                .toList();

        GroupDetailsResponse response = new GroupDetailsResponse(group.getId(), group.getGroupName(), group.getKod(), users);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deliver-key")
    public ResponseEntity<Void> deliverKey(@RequestHeader("Authorization") String header, @RequestBody DeliverKeyRequest request) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(token);
        User user = userService.authentification(userId);
        Long groupId = user.getGroup().getId();
        GroupKey gk = groupKeyRepository.findByGroupIdAndUserId(groupId, request.getTargetUserId()).orElseThrow();
        if (!"PENDING".equals(gk.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        gk.setEncryptedGroupKey(request.getEncryptedKey());
        gk.setStatus("ACTIVE");
        groupKeyRepository.save(gk);

        webSocketHandler.notifyKeyReady(request.getTargetUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-key")
    public ResponseEntity<String> getMyKey(@RequestHeader("Authorization") String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = header.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(token);
        User user = userService.authentification(userId);
        Long groupId = user.getGroup().getId();

        GroupKey gk = groupKeyRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow();

        if (!"ACTIVE".equals(gk.getStatus())) {
            return ResponseEntity.status(202).body("PENDING");
        }
        return ResponseEntity.ok(gk.getEncryptedGroupKey());
    }
}