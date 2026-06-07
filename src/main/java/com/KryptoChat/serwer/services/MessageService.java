package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.Message;
import com.KryptoChat.serwer.repositories.MessageRepository;
import org.springframework.stereotype.Service;

/**
 * Serwis odpowiedzialny za obsługę wiadomości w systemie czatu.
 * Umożliwia zapisywanie wiadomości do bazy danych.
 */
@Service
public class MessageService {

    private final MessageRepository repository;

    /**
     * Konstruktor inicjujący serwis wiadomości.
     *
     * @param repository repozytorium wiadomości używane do operacji na bazie danych
     */
    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }


    /**
     * Zapisuje wiadomość w bazie danych.
     *
     * @param message obiekt wiadomości do zapisania
     * @return zapisany obiekt Message (z nadanym ID)
     */
    public Message save(Message message) {
        return repository.save(message);
    }
}