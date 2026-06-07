package com.KryptoChat.serwer.services;
import com.KryptoChat.serwer.DTO.MessageList;
import com.KryptoChat.serwer.entities.Message;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import com.KryptoChat.serwer.repositories.MessageRepository;

/**
 * Serwis odpowiedzialny za obsługę czatu grupowego.
 * Udostępnia metody do pobierania historii wiadomości dla określonej grupy użytkowników.
 */
@Service
public class ChatService {

    private final MessageRepository messageRepository;

    public ChatService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }


    /**
     * Metoda pobierająca wszystkie wiadomości dla danej grupy.
     * Zwraca je w formie obiektu DTO MessageList.
     *
     * @param groupId identyfikator grupy, której wiadomości mają zostać pobrane
     * @return MessageList zawierający listę wiadomości
     */
    public MessageList loadMessages(Long groupId) {

        List<Message> messages = messageRepository.findByGroupId(groupId);

        return new MessageList(messages);
    }
}
