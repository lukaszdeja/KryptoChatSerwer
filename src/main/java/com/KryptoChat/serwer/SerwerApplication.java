package com.KryptoChat.serwer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;


/**
 * Główna klasa uruchomieniowa aplikacji Spring Boot.
 * Inicjalizuje kontekst aplikacji oraz ustawia domyślną strefę czasową na Europe/Warsaw przed uruchomieniem serwera.
 */
@SpringBootApplication
public class SerwerApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Warsaw"));
		SpringApplication.run(SerwerApplication.class, args);
	}

}
