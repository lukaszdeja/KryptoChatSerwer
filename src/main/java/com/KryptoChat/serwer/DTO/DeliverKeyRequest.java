package com.KryptoChat.serwer.DTO;

/**
 * DTO wykorzystywane do przekazania zaszyfrowanego klucza grupowego
 * do konkretnego użytkownika w procesie dystrybucji kluczy.
 */
public class DeliverKeyRequest {

    /**
     * Identyfikator użytkownika, do którego ma zostać dostarczony klucz.
     */
    private Long targetUserId;

    /**
     * Zaszyfrowany klucz grupowy przeznaczony dla użytkownika docelowego.
     */
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
