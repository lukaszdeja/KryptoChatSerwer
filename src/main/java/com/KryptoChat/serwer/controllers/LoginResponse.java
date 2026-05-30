package com.KryptoChat.serwer.controllers;

/**
 * Klasa DTO odpowiedzi zwrotnej na REST logowania
 */
public class LoginResponse {

    private UserCredentials userCredentials;
    private String message;
    private String jwt;
    private String encryptedPrivateKey;
    private String publicKey;

    /**
     * Konstruktor inicjujący pola klasy
     * @param userCredentials
     * @param jwt
     * @param message
     */
    public LoginResponse(UserCredentials userCredentials, String jwt, String message) {
        this.userCredentials = userCredentials;
        this.message = message;
        this.jwt = jwt;
    }
    //gettery potrzebne do serializacji i deserializacji
    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    public String getMessage() {
        return message;
    }

    public String getJwt() { return jwt;}
    //setter jwt
    public void setJwt(String jwt) { this.jwt = jwt;}

    public String getEncryptedPrivateKey() { return encryptedPrivateKey; }

    public void setEncryptedPrivateKey(String key) { encryptedPrivateKey = key; }

    public String getPublicKey() { return publicKey; }

    public void setPublicKey(String key) { publicKey = key; }
}

/**
 * Klasa pomocnicza zawierająca credentials użytkownika, jest używana przez Login Response
 */
class UserCredentials {
    private Long id;
    private String username;
    private Long groupId;

    /**
     * Konstruktor inicjująca pola klasy
     * @param id
     * @param username
     * @param groupId
     */
    public UserCredentials(Long id, String username, Long groupId) {
        this.id = id;
        this.username = username;
        this.groupId = groupId;
    }

    /**
     * Konstruktor bezparametrowy potrzebny do serializacji i deserializacji
     */
    public UserCredentials() {}
    //gettery potrzebne do serializacji i deserializacji
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Long getGroupId() {
        return groupId;
    }
}
