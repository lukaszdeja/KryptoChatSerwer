package com.KryptoChat.serwer.DTO;

public class DeliverKeyRequest {
    private Long targetUserId;
    private String encryptedKey;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long id) {
        targetUserId = id;
    }

    public String getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(String key) { encryptedKey = key; }

}
