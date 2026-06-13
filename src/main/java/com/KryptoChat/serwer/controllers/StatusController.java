package com.KryptoChat.serwer.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ta klasa służy tylko i wyłącznie temu, żeby po uruchomieniu głównego linku serwera:
 * https://kryptochatserwer-production.up.railway.app/ nie był wyświetlany Error tylko informacja
 * o poprawnym statusie działania aplikacji
 */
@RestController
public class StatusController {

    @GetMapping("/")
    public String status() {
        return "Aplikacja serwerowa projektu KryptoChat jest uruchomiona i działa poprawnie";
    }
}