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


@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

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
            JWTService jwtService = new JWTService();
            String token = jwtService.generateToken(user);
            response = new LoginResponse(res, token, "Zalogowano");
            System.out.println(response.getUserCredentials().getUsername());


        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        JWTService jwtService = new JWTService();

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtService.extractUserId(token);
        User user = userService.authentification(userId);

        return ResponseEntity.ok(user);
    }
}