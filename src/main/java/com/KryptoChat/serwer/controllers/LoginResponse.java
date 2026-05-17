package com.KryptoChat.serwer.controllers;

public class LoginResponse {

    private UserCredentials userCredentials;
    private String message;
    private String jwt;

    public LoginResponse(UserCredentials userCredentials, String jwt, String message) {
        this.userCredentials = userCredentials;
        this.message = message;
        this.jwt = jwt;
    }

    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    public String getMessage() {
        return message;
    }

    public String getJwt() { return jwt;}
    public void setJwt(String jwt) { this.jwt = jwt;}
}

class UserCredentials {
    private Long id;
    private String username;
    private Long groupId;

    public UserCredentials(Long id, String username, Long groupId) {
        this.id = id;
        this.username = username;
        this.groupId = groupId;
    }

    public UserCredentials() {}

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
