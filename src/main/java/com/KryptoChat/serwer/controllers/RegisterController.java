package com.KryptoChat.serwer.controllers;
import com.KryptoChat.serwer.DTO.RegisterRequest;
import jakarta.validation.Valid;
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
     * Metoda zapisująca użytkownika w bazie danych
     * wraz z publicznym kluczem RSA
     * i zwracająca odpowiedź serwera
     * @param request request rejestracji
     * @return ResponseEntity
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid  @RequestBody RegisterRequest request
    ) {
        if (request.getUsername().length() > 20 || request.getPassword().length() > 30) {
            return ResponseEntity.badRequest().body("Login lub haslo jest zbyt dlugie");
        }
        userService.register(request.getUsername(), request.getPassword(), request.getPublicKey(), request.getEncryptedPrivateKey());
        return ResponseEntity.ok("Uzytkownik sie zarejestrowal");
    }
}

