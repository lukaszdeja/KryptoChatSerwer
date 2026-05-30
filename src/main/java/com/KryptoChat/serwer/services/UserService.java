package com.KryptoChat.serwer.services;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.KryptoChat.serwer.repositories.*;
import com.KryptoChat.serwer.entities.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Rejestracja nowego użytkownika
     * Tworzy konto użytkownika oraz zapisuje jego publiczny klucz RSA
     */
    public void register(String username, String password, String publicKey, String encryptedPrivateKey) {

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Użytkownik o podanej nazwie już istnieje");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User user = new User(username, hashedPassword);

        user.setPublicKey(publicKey);
        user.setEncryptedPrivateKey(encryptedPrivateKey);

        userRepository.save(user);
    }

    /**
     * Logowanie użytkownika
     */
    public User login(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nie ma takiego użytkownika"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Błędne hasło");
        }

        return user;
    }

    public User authentification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user;
    }

    /**
     * Pobranie użytkownika po username
     * (używane np. w GroupController)
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));
    }
}