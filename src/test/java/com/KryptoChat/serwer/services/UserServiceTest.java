package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;


    @Test
    @DisplayName("register zapisuje użytkownika gdy username jest wolny")
    void register_NewUsername_SavesUser() {
        when(userRepository.existsByUsername("jan")).thenReturn(false);
        when(passwordEncoder.encode("haslo")).thenReturn("hashed");

        userService.register("jan", "haslo", "pubKey", "encPrivKey");

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register koduje hasło przed zapisem")
    void register_EncodesPasswordBeforeSaving() {
        when(userRepository.existsByUsername("jan")).thenReturn(false);
        when(passwordEncoder.encode("haslo")).thenReturn("hashed");

        userService.register("jan", "haslo", "pubKey", "encPrivKey");

        verify(passwordEncoder, times(1)).encode("haslo");
    }

    @Test
    @DisplayName("register rzuca RuntimeException gdy username już istnieje")
    void register_DuplicateUsername_ThrowsRuntimeException() {
        when(userRepository.existsByUsername("jan")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register("jan", "haslo", "pubKey", "encPrivKey"));

        assertThat(ex.getMessage()).contains("już istnieje");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register nie wywołuje save gdy username już istnieje")
    void register_DuplicateUsername_NeverCallsSave() {
        when(userRepository.existsByUsername("jan")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.register("jan", "haslo", "pubKey", "encPrivKey"));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }


    @Test
    @DisplayName("login zwraca użytkownika przy poprawnych danych")
    void login_ValidCredentials_ReturnsUser() {
        User user = new User("jan", "hashed");
        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("haslo", "hashed")).thenReturn(true);
        User result = userService.login("jan", "haslo");
        assertThat(result.getUsername()).isEqualTo("jan");
        assertThat(result.getPassword()).isEqualTo("hashed");
    }

    @Test
    @DisplayName("login rzuca RuntimeException gdy użytkownik nie istnieje")
    void login_UnknownUsername_ThrowsRuntimeException() {
        when(userRepository.findByUsername("nieznany")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("nieznany", "haslo"));

        assertThat(ex.getMessage()).contains("Nie ma takiego użytkownika");
    }

    @Test
    @DisplayName("login rzuca RuntimeException przy błędnym haśle")
    void login_WrongPassword_ThrowsRuntimeException() {
        User user = new User("jan", "hashed");
        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("zle_haslo", "hashed")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("jan", "zle_haslo"));

        assertThat(ex.getMessage()).contains("Błędne hasło");
    }

    @Test
    @DisplayName("login nie sprawdza hasła gdy użytkownik nie istnieje")
    void login_UnknownUsername_NeverChecksPassword() {
        when(userRepository.findByUsername("nieznany")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.login("nieznany", "haslo"));

        verifyNoInteractions(passwordEncoder);
    }


    @Test
    @DisplayName("authentification zwraca użytkownika dla istniejącego id")
    void authentification_ExistingId_ReturnsUser() {
        User user = new User("jan", "hashed");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.authentification(1L);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("authentification rzuca RuntimeException gdy użytkownik nie istnieje")
    void authentification_UnknownId_ThrowsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.authentification(99L));

        assertThat(ex.getMessage()).contains("Nie znalezniono użytkownika");
    }


}