package com.KryptoChat.serwer.services;
import com.KryptoChat.serwer.controllers.MessageList;
import com.KryptoChat.serwer.entities.Message;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import com.KryptoChat.serwer.repositories.MessageRepository;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    public ChatService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageList loadMessages(Long groupId) {

        List<Message> messages = messageRepository.findByGroupId(groupId);

        return new MessageList(messages);
    }
}
