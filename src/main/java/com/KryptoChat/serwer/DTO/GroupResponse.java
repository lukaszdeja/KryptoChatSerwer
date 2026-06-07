package com.KryptoChat.serwer.DTO;

/**
 * Klasa realizująca DTO dołączania bądź tworzenia grupy
 */
public class GroupResponse {


    /**
     * Wiadomość zwrotna z serwera informująca o wyniku operacji
     * (np. "Utworzono grupę", "Dołączono do grupy", błąd operacji).
     */
    private String message;

    /**
     * Nowy token JWT wygenerowany po operacji (np. po utworzeniu lub dołączeniu do grupy),
     * zawierający zaktualizowane informacje o użytkowniku.
     */
    private String jwt;

    /**
     * Dane uwierzytelniające użytkownika zwracane do klienta,
     * zawierające m.in. identyfikator, nazwę użytkownika oraz ID grupy.
     */
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


    // settery
    public void setJwt(String jwt) {this.jwt = jwt;}
    public void setMessage(String message) { this.message = message; }
    public void setUserCredentials(UserCredentials userCredentials) { this.userCredentials = userCredentials;}
}
