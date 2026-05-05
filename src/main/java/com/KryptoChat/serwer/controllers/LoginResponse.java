package com.KryptoChat.serwer.controllers;

public class LoginResponse {

    private UserToken userToken;
    private String message;

    public LoginResponse(UserToken userToken, String message) {
        this.userToken = userToken;
        this.message = message;
    }

    public UserToken getUserToken() {
        return userToken;
    }

    public String getMessage() {
        return message;
    }
}

class UserToken {
    private Long id;
    private String username;
    private Long groupId;

    public UserToken(Long id, String username, Long groupId) {
        this.id = id;
        this.username = username;
        this.groupId = groupId;
    }

    public UserToken() {}

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
