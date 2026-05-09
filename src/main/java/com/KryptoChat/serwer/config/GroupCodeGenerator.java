package com.KryptoChat.serwer.config;

import java.security.SecureRandom;

public class GroupCodeGenerator {

    private static final String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    public static String generateCode() {
        StringBuilder code = new StringBuilder("#");

        for (int i=0; i<5; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }
}
