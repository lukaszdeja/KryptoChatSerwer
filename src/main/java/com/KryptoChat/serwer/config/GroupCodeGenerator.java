package com.KryptoChat.serwer.config;

import java.security.SecureRandom;

/**
 *Klasa pomocnicza generująca losowe kody dołączenia do grupy
 */
public class GroupCodeGenerator {

    private static final String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Metoda statyczna generująca i zwracająca losowy kod zaczynający się od #, a dalej z 5 losowych
     * liter bądź cyfr
     * @return String code - wygenerowany kod
     */
    public static String generateCode() {
        StringBuilder code = new StringBuilder("#");

        for (int i=0; i<5; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }
}
