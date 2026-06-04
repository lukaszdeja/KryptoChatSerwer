package com.KryptoChat.serwer.DTO;

/**
 * Klasa pomocnicza zawierająca credentials użytkownika, jest używana przez Login Response
 */
public class UserCredentials {
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