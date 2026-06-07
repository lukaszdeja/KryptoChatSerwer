package com.KryptoChat.serwer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.KryptoChat.serwer.entities.*;
import java.util.Optional;

/**
 * Repozytorium odpowiedzialne za wykonywanie operacji na encji User w bazie danych.
 * Umożliwia zarządzanie użytkownikami oraz wyszukiwanie ich na podstawie nazwy użytkownika.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}