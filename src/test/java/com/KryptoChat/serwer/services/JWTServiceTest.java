package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UWAGA - ta klasa realizuje testy jednostkowe dla serwisu JWTService
 * Klasa JWTService korzysta ze zmiennej środowiskowej JWT_SECRET
 * Aby testy przeszły poprawnie, konieczne jest zdefiniowanie zmiennej środowiskowej JWT_SECRET jako ciąg znaków nie krótszy niz 32 znaki
 */
class JWTServiceTest {

    private JWTService jwtService;

    private User user;
    private Group group;

    /**
     * Setup przed kazdym testem - zbudowanie testowanych obiektow
     */
    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        group = new Group();
        group.setId(100L);
        user = new User();
        user.setId(1L);
        user.setUsername("lukasz");
        user.setGroup(group);
    }

    /**
     * Test sprawdzajacy czy zostal wygenerowany token - niepusty
     */
    @Test
    void shouldGenerateToken() {
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    /**
     * Test sprawdzajacy czy wygenerowany token jest prawidlowy
     */
    @Test
    void shouldValidateGeneratedToken() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token));
    }

    /**
     * Test sprawdzajacy nieprawidlowy token - wyjdzie ze nie jest Valid
     */
    @Test
    void shouldReturnFalseForInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid-token"));
    }

    /**
     * Test sprawdzajacy czy prawidlowo dziala wyciaganie id uzytkownika z tokenu
     */
    @Test
    void shouldExtractUserId() {
        String token = jwtService.generateToken(user);
        Long userId = jwtService.extractUserId(token);
        assertEquals(1L, userId);
    }

    /**
     * Test sprawdzajacy czy prawidlowo dziala wyciaganie username z tokenu
     */
    @Test
    void shouldExtractUsername() {
        String token = jwtService.generateToken(user);
        String username = jwtService.extractUsername(token);
        assertEquals("lukasz", username);
    }

    /**
     * Test sprawdzajacy czy prawidlowo dziala wyciaganie id grupy z tokenu
     */
    @Test
    void shouldExtractGroupId() {
        String token = jwtService.generateToken(user);
        Long groupId = jwtService.extractGroupId(token);
        assertEquals(100L, groupId);
    }

    /**
     * Test sprawdzajacy czy dla uzytkownika ktory nie ma grupy zadnej, id grupy w tokenie jest null
     */
    @Test
    void shouldReturnNullWhenUserHasNoGroup() {
        user.setGroup(null);
        String token = jwtService.generateToken(user);
        assertNull(jwtService.extractGroupId(token));
    }

    /**
     * Test sprawdzajacy czy dla innego usera nie zostanie wygenerowany taki sam token
     */
    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        User secondUser = new User();
        secondUser.setId(2L);
        secondUser.setUsername("adam");
        String token1 = jwtService.generateToken(user);
        String token2 = jwtService.generateToken(secondUser);
        assertNotEquals(token1, token2);
    }

    /**
     * Test sprawdzajacy czy dane wyciagniete z wygenerowanego tokenu sa zgodne z rzeczywistymi danymi uzytkownika
     */
    @Test
    void extractedDataShouldMatchOriginalUserData() {
        String token = jwtService.generateToken(user);
        assertAll(
                () -> assertEquals(user.getId(),
                        jwtService.extractUserId(token)),
                () -> assertEquals(user.getUsername(),
                        jwtService.extractUsername(token)),
                () -> assertEquals(user.getGroup().getId(),
                        jwtService.extractGroupId(token))
        );
    }
}