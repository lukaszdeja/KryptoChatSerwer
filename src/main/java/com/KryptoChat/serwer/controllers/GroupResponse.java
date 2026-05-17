package com.KryptoChat.serwer.controllers;

public class GroupResponse {


    private String message;
    private String jwt;
    private UserCredentials userCredentials;
    public GroupResponse() {}

    public GroupResponse(String jwt, UserCredentials userCredentials, String message) {
        this.jwt = jwt;
        this.userCredentials = userCredentials;
        this.message = message;
    }

    // gettery
    public String getJwt() { return this.jwt; }
    public String getMessage() { return message; }

    // settery
    public void setJwt(String jwt) {this.jwt = jwt;}
    public void setMessage(String message) { this.message = message; }
}
