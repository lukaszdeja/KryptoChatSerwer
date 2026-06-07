package com.KryptoChat.serwer.repositories;

import com.KryptoChat.serwer.entities.GroupKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium odpowiedzialne za wykonywanie operacji na encji GroupKey w bazie danych.
 * Umożliwia wyszukiwanie kluczy grupowych przypisanych do użytkowników oraz filtrowanie ich po statusie.
 */
public interface GroupKeyRepository extends JpaRepository<GroupKey, Long> {

    Optional<GroupKey> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupKey> findByGroupIdAndStatus(Long groupId, String status);
}