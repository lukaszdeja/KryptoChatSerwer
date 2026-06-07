package com.KryptoChat.serwer.DTO;

/**
 * DTO zwracania danych uzytkownikow w liscie w grupie
 */
public class UserResponse {

    private Long id;
    private String username;

    /**
     * Konstruktor inicjujący pola klasy
     * @param id
     * @param username
     */
    public UserResponse(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
}