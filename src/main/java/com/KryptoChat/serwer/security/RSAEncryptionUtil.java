package com.KryptoChat.serwer.security;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Util odpowiedzialny za szyfrowanie danych kluczem publicznym RSA.
 */
public class RSAEncryptionUtil {

    /**
     * Szyfruje dane kluczem publicznym RSA (Base64).
     *
     * @param data dane do zaszyfrowania (np. AES key)
     * @param publicKey klucz publiczny użytkownika (Base64)
     * @return zaszyfrowany tekst (Base64)
     */
    public static String encryptRSA(String data, String publicKey) {

        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

            KeyFactory factory = KeyFactory.getInstance("RSA");

            PublicKey pk = factory.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.ENCRYPT_MODE, pk);

            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(data.getBytes())
            );

        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }
}