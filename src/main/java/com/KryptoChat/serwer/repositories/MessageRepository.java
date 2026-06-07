package com.KryptoChat.serwer.repositories;

import com.KryptoChat.serwer.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repozytorium odpowiedzialne za wykonywanie operacji na encji Message w bazie danych.
 * Umożliwia zapisywanie, usuwanie oraz pobieranie wiadomości przypisanych do grup użytkowników.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByGroupId(Long groupId);
}