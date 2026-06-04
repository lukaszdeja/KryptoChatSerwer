package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.Message;
import com.KryptoChat.serwer.repositories.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository repository;

    @InjectMocks
    private MessageService messageService;

    @Test
    @DisplayName("save wywołuje repository.save z przekazaną wiadomością")
    void save_CallsRepositorySaveWithCorrectMessage() {
        Message message = new Message();
        when(repository.save(message)).thenReturn(message);

        messageService.save(message);

        verify(repository, times(1)).save(message);
    }

    @Test
    @DisplayName("save zwraca wiadomość zwróconą przez repozytorium")
    void save_ReturnsMessageFromRepository() {
        Message message = new Message();
        Message savedMessage = new Message();
        when(repository.save(message)).thenReturn(savedMessage);

        Message result = messageService.save(message);

        assertThat(result).isSameAs(savedMessage);
    }

    @Test
    @DisplayName("save nie wywołuje żadnych innych metod repozytorium")
    void save_NoOtherRepositoryInteractions() {
        Message message = new Message();
        when(repository.save(message)).thenReturn(message);

        messageService.save(message);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("save propaguje wyjątek rzucony przez repozytorium")
    void save_RepositoryThrowsException_PropagatesException() {
        Message message = new Message();
        when(repository.save(message)).thenThrow(new RuntimeException("Błąd zapisu"));

        assertThrows(RuntimeException.class, () -> messageService.save(message));
    }
}