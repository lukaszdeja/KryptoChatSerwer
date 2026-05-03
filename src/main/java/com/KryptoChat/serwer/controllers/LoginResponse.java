package com.KryptoChat.serwer.controllers;

public class LoginResponse {

    private Long id;
    private String username;
    private String message;

    public LoginResponse(Long id, String username, String message) {
        this.id = id;
        this.username = username;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }
}
