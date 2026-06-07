package com.KryptoChat.serwer.entities;

import jakarta.persistence.*;


/**
 * Encja reprezentująca zaszyfrowany klucz grupowy przypisany do użytkownika.
 * Przechowuje identyfikator grupy, identyfikator użytkownika, zaszyfrowany
 * klucz grupowy oraz jego aktualny status w procesie dystrybucji kluczy.
 */
@Entity
@Table(name = "group_keys")
public class GroupKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupId;

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String encryptedGroupKey;

    private String status;

    public GroupKey() {}

    public GroupKey(Long groupId, Long userId, String encryptedGroupKey) {
        this.groupId = groupId;
        this.userId = userId;
        this.encryptedGroupKey = encryptedGroupKey;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEncryptedGroupKey() {
        return encryptedGroupKey;
    }

    public void setEncryptedGroupKey(String encryptedGroupKey) {
        this.encryptedGroupKey = encryptedGroupKey;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}