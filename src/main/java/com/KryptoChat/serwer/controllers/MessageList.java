package com.KryptoChat.serwer.controllers;
import java.util.List;
import java.util.ArrayList;
import com.KryptoChat.serwer.entities.Message;


public class MessageList {

    private List<Message> messages;

    public MessageList() {
    }

    public MessageList(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}