package com.KryptoChat.serwer.DTO;
import java.util.List;
import java.util.ArrayList;
import com.KryptoChat.serwer.entities.Message;

/**
 * Klasa opakowująca DTO do zwracania listy wiadomości z bazy danych
 */
public class MessageList {

    private List<Message> messages;

    /**
     * Konstruktor bezparametrowy potrzebny do serializacji i deserializacji
     */
    public MessageList() {
    }

    /**
     * Konstruktor inicjujący listę wiadomości
     * @param messages
     */
    public MessageList(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Getter zwracający liste
     * @return List
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Setter ustawiający listę
     * @param messages
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}