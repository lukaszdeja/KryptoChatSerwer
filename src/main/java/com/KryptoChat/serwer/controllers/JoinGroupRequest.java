package com.KryptoChat.serwer.controllers;

public class JoinGroupRequest {

    private String username;
    private String code;

    public JoinGroupRequest() {}

    public String getUsername() { return username; }
    public String getCode() { return code; }
    public void setUsername(String username) { this.username = username; }
    public void setCode(String code) { this.code = code; }
}
