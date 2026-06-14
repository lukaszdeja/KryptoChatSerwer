package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.DTO.LoginResponse;
import com.KryptoChat.serwer.DTO.RegisterRequest;
import com.KryptoChat.serwer.DTO.UserCredentials;
import com.KryptoChat.serwer.repositories.UserRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.*;
import com.KryptoChat.serwer.entities.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Klasa realizująca kontroler Logowania obsługujący żądania REST
 * Realizuje logowanie oraz autentyfikacje
 * Endpoint me - walidacja zapisanego juz tokenu przy ponownym uruchomieniu aplikacji
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserService userService;
    private final JWTService jwtService;

    /**
     * Konstruktor inicjujący pole serwisu użytkownika
     * @param userService
     */
    public LoginController(UserService userService, JWTService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Metoda obsługująca żądania post logowania, sprawdza login i hasło w bazie danych
     * Jeżeli są poprawne tworzy zwracany obiekt zawierający token jwt oraz credentials potrzebne frontendowi
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody RegisterRequest request) throws BadRequestException {
        User user;
        LoginResponse response;
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new LoginResponse(null, null, "Login i haslo sa wymagane"));
        }
        if (request.getUsername().length() > 20 || request.getUsername().length() > 30) {
            return ResponseEntity.badRequest().body(new LoginResponse(null, null, "Login lub haslo sa zbyt dlugie"));
        }
        user = userService.login(request.getUsername(), request.getPassword());
        Long groupId;
        try {
            groupId = user.getGroup().getId();
        } catch (NullPointerException e) {
            groupId = null;
        }
        UserCredentials res = new UserCredentials(user.getId(), user.getUsername(), groupId);
        String token = jwtService.generateToken(user);
        response = new LoginResponse(res, token, "Zalogowano");
        response.setEncryptedPrivateKey(user.getEncryptedPrivateKey());
        response.setPublicKey(user.getPublicKey());
        System.out.println(response.getUserCredentials().getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Metoda obsługująca endpoint get /me, czyli autentyfikację tokenu po uruchomieniu aplikacji
     * Jeżeli token jest prawidłowy, zwróci obiekt zalogowanego użytkownika
     * @param authHeader
     * @return ResponseEntity
     */
    @GetMapping("/me")
    public ResponseEntity<UserCredentials> me(@RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtService.extractUserId(token);
        User user = userService.authentification(userId);
        Long groupId = null;
        if (user.getGroup() != null) {
            groupId = user.getGroup().getId();
        }

        return ResponseEntity.ok(new UserCredentials(user.getId(), user.getUsername(), groupId));
    }
}