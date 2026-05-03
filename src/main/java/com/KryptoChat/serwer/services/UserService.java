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

    public void register(String username, String password) {

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Użytkownik o podanej nazwie już istnieje");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User user = new User(username, hashedPassword);
        userRepository.save(user);
    }

    public User login(String username, String password) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Nie ma takiego użytkownika"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Błędne hasło");
        }

        return user;
    }
}
