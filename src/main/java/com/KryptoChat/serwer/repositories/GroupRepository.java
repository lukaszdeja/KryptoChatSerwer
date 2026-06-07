package com.KryptoChat.serwer.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.KryptoChat.serwer.entities.*;
import java.util.Optional;

/**
 * Repozytorium odpowiedzialne za wykonywanie operacji na encji Group w bazie danych.
 * Umożliwia zarządzanie grupami oraz wyszukiwanie ich po nazwie lub kodzie dołączenia.
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByGroupName(String groupName);

    Optional<Group> findByKod(String kod);
}
