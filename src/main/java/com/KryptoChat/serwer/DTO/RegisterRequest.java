package com.KryptoChat.serwer.DTO;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO żądania rejestracji
 */
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(min = 8, max = 50)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
            message = "Hasło nie spełnia wymagań"
    )
    private String password;

    @NotBlank
    private String publicKey;

    @NotBlank
    private String encryptedPrivateKey;


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
    //getter klucza publicznego
    public String getPublicKey() {
        return publicKey;
    }
    //setter klucza publicznego
    public void setPublicKey(String key) {
        this.publicKey = key;
    }

    public void setEncryptedPrivateKey(String key) { this.encryptedPrivateKey = key; }

    public String getEncryptedPrivateKey() { return encryptedPrivateKey; }
}
