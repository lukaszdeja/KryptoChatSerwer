package com.KryptoChat.serwer.controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.*;

/**
 * Klasa obsługująca kontroler rejestracji obsługujący żądania REST rejestracji użytkowników
 */
@RestController
@RequestMapping("/api")
public class RegisterController {

    private final UserService userService;

    /**
     * Konstruktor inicjujący serwis użytkownika
     * @param userService
     */
    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Metoda zappisująca użytkownika w bazie danych i zwracająca odpowiedź serwera
     * @param request
     * @return ResponseEntity
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {

        userService.register(request.getUsername(), request.getPassword());

        return ResponseEntity.ok("Użytkownik sie zarejestrował");
    }
}

