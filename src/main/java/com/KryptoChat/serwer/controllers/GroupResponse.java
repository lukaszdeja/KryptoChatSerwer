package com.KryptoChat.serwer.controllers;

/**
 * Klasa realizująca DTO dołączania bądź tworzenia grupy
 */

public class GroupResponse {


    private String message;
    private String jwt;
    private UserCredentials userCredentials;
    public GroupResponse() {}

    /**
     * Konstruktor inicjujący pola klasy
     * @param jwt
     * @param userCredentials
     * @param message
     */
    public GroupResponse(String jwt, UserCredentials userCredentials, String message) {
        this.jwt = jwt;
        this.userCredentials = userCredentials;
        this.message = message;
    }

    // gettery
    public String getJwt() { return this.jwt; }
    public String getMessage() { return message; }
    public UserCredentials getUserCredentials() { return this.userCredentials;}
    public void setUserCredentials(UserCredentials userCredentials) { this.userCredentials = userCredentials;}

    // settery
    public void setJwt(String jwt) {this.jwt = jwt;}
    public void setMessage(String message) { this.message = message; }
}
