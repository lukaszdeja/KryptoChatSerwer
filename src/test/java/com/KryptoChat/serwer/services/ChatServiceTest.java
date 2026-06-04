package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.DTO.MessageList;
import com.KryptoChat.serwer.entities.Message;
import com.KryptoChat.serwer.repositories.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("loadMessages wywołuje repository.findByGroupId z przekazanym groupId")
    void loadMessages_CallsRepositoryWithCorrectGroupId() {
        Long groupId = 1L;
        when(messageRepository.findByGroupId(groupId)).thenReturn(Collections.emptyList());

        chatService.loadMessages(groupId);

        verify(messageRepository, times(1)).findByGroupId(groupId);
    }

    @Test
    @DisplayName("loadMessages zwraca MessageList zawierający wiadomości z repozytorium")
    void loadMessages_ReturnsMessageListWithMessagesFromRepository() {
        Long groupId = 1L;
        Message msg1 = new Message();
        Message msg2 = new Message();
        List<Message> messages = List.of(msg1, msg2);
        when(messageRepository.findByGroupId(groupId)).thenReturn(messages);

        MessageList result = chatService.loadMessages(groupId);

        assertThat(result).isNotNull();
        assertThat(result.getMessages()).containsExactlyElementsOf(messages);
    }

    @Test
    @DisplayName("loadMessages zwraca pusty MessageList gdy brak wiadomości w grupie")
    void loadMessages_NoMessages_ReturnsEmptyMessageList() {
        Long groupId = 99L;
        when(messageRepository.findByGroupId(groupId)).thenReturn(Collections.emptyList());

        MessageList result = chatService.loadMessages(groupId);

        assertThat(result).isNotNull();
        assertThat(result.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("loadMessages nie wywołuje żadnych innych metod repozytorium")
    void loadMessages_NoOtherRepositoryInteractions() {
        Long groupId = 1L;
        when(messageRepository.findByGroupId(groupId)).thenReturn(Collections.emptyList());

        chatService.loadMessages(groupId);

        verifyNoMoreInteractions(messageRepository);
    }

    @Test
    @DisplayName("loadMessages propaguje wyjątek rzucony przez repozytorium")
    void loadMessages_RepositoryThrowsException_PropagatesException() {
        Long groupId = 1L;
        when(messageRepository.findByGroupId(groupId)).thenThrow(new RuntimeException("Błąd bazy danych"));

        assertThrows(RuntimeException.class, () -> chatService.loadMessages(groupId));
    }
}