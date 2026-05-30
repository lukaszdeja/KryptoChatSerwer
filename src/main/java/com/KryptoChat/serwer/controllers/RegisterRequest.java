package com.KryptoChat.serwer.controllers;

/**
 * DTO żądania rejestracji
 */
public class RegisterRequest {

    private String username;
    private String password;
    private String publicKey;
    private String encryptedPrivateKey;


    // getter nazwy użytkownika
    public String getUsername() {
        return username;
    }
    //setter nazwy użytkownika
    public void setUsername(String username) {
        this.username = username;
    }
    //getter hasła
    public String getPassword() {
        return password;
    }
    //setter hasła
    public void setPassword(String password) {
        this.password = password;
    }
    //getter klucza publicznego
    public String getPublicKey() {
        return publicKey;
    }
    //setter klucza publicznego
    public void setPublicKey(String key) {
        this.publicKey = key;
    }

    public void setEncryptedPrivateKey(String key) { this.encryptedPrivateKey = key; }

    public String getEncryptedPrivateKey() { return encryptedPrivateKey; }
}
