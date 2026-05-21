package com.KryptoChat.serwer.controllers;

/**
 * DTO żądania rejestracji
 */
public class RegisterRequest {

    private String username;
    private String password;
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
}
