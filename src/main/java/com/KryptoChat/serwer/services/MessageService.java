package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.Message;
import com.KryptoChat.serwer.repositories.MessageRepository;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    public Message save(Message message) {
        return repository.save(message);
    }
}