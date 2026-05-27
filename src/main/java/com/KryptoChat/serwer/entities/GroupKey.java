package com.KryptoChat.serwer.entities;

import jakarta.persistence.*;

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

    public Long getUserId() {
        return userId;
    }

    public String getEncryptedGroupKey() {
        return encryptedGroupKey;
    }

    public void setEncryptedGroupKey(String encryptedGroupKey) {
        this.encryptedGroupKey = encryptedGroupKey;
    }
}
