package com.KryptoChat.serwer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GroupCodeGeneratorTest {

    /**
     * Metoda testująca sprawdzająca czy wygenerowany kod zaczyna się od #
     */
    @Test
    void shouldReturnCodeStartingWithHash() {
        String code = GroupCodeGenerator.generateCode();
        assertTrue(code.startsWith("#"), "Kod powinien zaczynać się od '#', ale był: " + code);
    }

    /**
     * Metoda testująca sprawdzająca czy wygenerowany kod ma odpowiednią długośc - 6 znaków
     */
    @Test
    void shouldReturnCodeWithTotalLengthOfSix() {
        String code = GroupCodeGenerator.generateCode();
        assertEquals(6, code.length(), "Kod powinien mieć długość 6 znaków (# + 5 znaków), ale miał: " + code.length());
    }


    /**
     * Metoda testująca sprawdzająca czy kod składa się z poprawnych znaków - malych liter i cyfr
     */
    @Test
    void shouldContainOnlyAllowedCharactersAfterHash() {
        String allowedChars = "abcdefghijklmnopqrstuvwxyz0123456789";
        String code = GroupCodeGenerator.generateCode();
        String suffix = code.substring(1);

        for (char c : suffix.toCharArray()) {
            assertTrue(
                    allowedChars.indexOf(c) >= 0,
                    "Niedozwolony znak '" + c + "' w kodzie: " + code
            );
        }
    }

    /**
     * Metoda testująca sprawdzająca czy wygenerowany kod nie jest nullem
     */
    @RepeatedTest(10)
    void shouldReturnNonNullCode() {
        assertNotNull(GroupCodeGenerator.generateCode(), "Wygenerowany kod nie powinien być null");
    }

    /**
     * Metoda testujaca sprawdzajaca czy wygenerowane kody sa wzglednie unikalne - margines powtorzonych = 2%
     */
    @Test
    void shouldGenerateUniqueCodesOverMultipleCalls() {

        int iterations = 200;
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            codes.add(GroupCodeGenerator.generateCode());
        }

        int minimumUnique = (int) (iterations * 0.98);
        assertTrue(codes.size() >= minimumUnique,
                "Zbyt wiele powtórzeń — wygenerowano tylko " + codes.size() + " unikalnych kodów z " + iterations
        );
    }

    /**
     * Metoda testujaca czy generowane sa rozne kody
     */
    @Test
    void shouldNotAlwaysReturnTheSameCode() {
        String first = GroupCodeGenerator.generateCode();
        String second = GroupCodeGenerator.generateCode();
        assertNotEquals(first, second, "Dwa kolejne kody nie powinny być identyczne");
    }

    /**
     * Metoda testujaca sprawdzajaca czy bedzie rzucony wyjatek
     */
    @Test
    void shouldBeCallableMultipleTimesWithoutException() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                GroupCodeGenerator.generateCode();
            }
        }, "Wielokrotne wywołanie generateCode() nie powinno rzucać wyjątku");
    }
}