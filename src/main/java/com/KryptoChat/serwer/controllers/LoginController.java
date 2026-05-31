package com.KryptoChat.serwer.controllers;

import com.KryptoChat.serwer.repositories.UserRepository;
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
    public ResponseEntity<LoginResponse> login(@RequestBody RegisterRequest request) {
        User user;
        LoginResponse response;
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