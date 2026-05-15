package com.KryptoChat.serwer.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Long groupId;

    private LocalDateTime send_time;

    public Message() {}

    public Message(String sender, String content, Long groupId, LocalDateTime send_time) {
        this.sender = sender;
        this.content = content;
        this.groupId = groupId;
        this.send_time = send_time;
    }

    public Long getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getTimestamp() {
        return send_time;
    }

    public void setTimestamp(LocalDateTime send_time) {
        this.send_time = send_time;
    }
}