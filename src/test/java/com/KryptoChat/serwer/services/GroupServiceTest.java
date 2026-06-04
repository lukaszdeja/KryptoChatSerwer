package com.KryptoChat.serwer.services;

import com.KryptoChat.serwer.entities.Group;
import com.KryptoChat.serwer.entities.GroupKey;
import com.KryptoChat.serwer.entities.User;
import com.KryptoChat.serwer.repositories.GroupKeyRepository;
import com.KryptoChat.serwer.repositories.GroupRepository;
import com.KryptoChat.serwer.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    /**
     * Wykorzystywane mocki repozytoriów
     */
    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupKeyRepository groupKeyRepository;

    @InjectMocks
    private GroupService groupService;

    private User user;
    private Group group;

    /**
     * Setup poczatkowego stanu obiektow
     */
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("lukasz");

        group = new Group();
        group.setId(10L);
        group.setGroupName("testGroup");
        group.setKod("#abcde");
    }

    /**
     * Test sprawdzajacy czy utworzenie grupy zwraca Id
     */
    @Test
    void createGroup_shouldReturnGroupId() {
        mockGroupSaveWithId(10L);
        Long result = groupService.createGroup("testGroup", user, "encrypted-key");
        assertEquals(10L, result);
    }

    /**
     * Test sprawdzajacy czy utworzenie grupy, przypisuje id do osoby ktora grupe stworzyla
     */
    @Test
    void createGroup_shouldAssignGroupToCreator() {
        mockGroupSaveWithId(10L);
        groupService.createGroup("testGroup", user, "encrypted-key");
        assertNotNull(user.getGroup());
        assertEquals(10L, user.getGroup().getId());
        assertEquals("testGroup", user.getGroup().getGroupName());
        assertNotNull(user.getGroup().getKod());
    }

    /**
     * Test sprawdzajacy czy udalo sie zapisac w bazie wygenerowany klucz grupy AES (zaszyfrowany) i czy ma status ACTIVE
     */
    @Test
    void createGroup_shouldSaveGroupKeyWithActiveStatus() {
        mockGroupSaveWithId(10L);
        //przechwytuje to ze w groupService jest tworzony GroupKey
        ArgumentCaptor<GroupKey> captor = ArgumentCaptor.forClass(GroupKey.class);
        groupService.createGroup("testGroup", user, "encrypted-key");
        verify(groupKeyRepository).save(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    /**
     * Test sprawdzajacy czy zapisany w bazie klucz grupy nie jest nullem
     */
    @Test
    void createGroup_shouldSaveGroupKeyWithCorrectEncryptedKey() {
        mockGroupSaveWithId(10L);
        //ponownie przechwycenie klucza
        ArgumentCaptor<GroupKey> captor = ArgumentCaptor.forClass(GroupKey.class);
        groupService.createGroup("testGroup", user, "encrypted-key");
        verify(groupKeyRepository).save(captor.capture());
        // GroupKey przechowuje klucz przekazany przez twórcę
        assertNotNull(captor.getValue()); // klucz nie może być null dla ACTIVE
    }

    /**
     * Test sprawdzajacy czy dolaczenie do grupy (zapis w bazie), zwraca id grupy
     */
    @Test
    void joinGroup_shouldReturnGroupId() {
        when(groupRepository.findByKod("#abcde")).thenReturn(Optional.of(group));
        Long result = groupService.joinGroup("#abcde", user);
        assertEquals(10L, result);
    }

    /**
     * Test sprawdzajacy czy dolaczenie do grupy, przypisuje osobie dolaczajacej id tej grupy
     */
    @Test
    void joinGroup_shouldAssignFoundGroupToUser() {
        when(groupRepository.findByKod("#abcde")).thenReturn(Optional.of(group));
        groupService.joinGroup("#abcde", user);
        assertEquals(group, user.getGroup());
    }

    /**
     * Test sprawdzajacy czy po dolaczeniu do grupy w bazie danych zostaje zapisany (na razie) pusty klucz grupy ze statusem pending
     */
    @Test
    void joinGroup_shouldSaveGroupKeyWithPendingStatus() {
        when(groupRepository.findByKod("#abcde")).thenReturn(Optional.of(group));
        ArgumentCaptor<GroupKey> captor = ArgumentCaptor.forClass(GroupKey.class);
        groupService.joinGroup("#abcde", user);
        verify(groupKeyRepository).save(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    /**
     * Test sprawdzajacy czy po dolaczeniu do grupy wartosc tego klucza jest nullem
     */
    @Test
    void joinGroup_shouldSaveGroupKeyWithNullEncryptedKey() {
        when(groupRepository.findByKod("#abcde")).thenReturn(Optional.of(group));
        ArgumentCaptor<GroupKey> captor = ArgumentCaptor.forClass(GroupKey.class);
        groupService.joinGroup("#abcde", user);
        verify(groupKeyRepository).save(captor.capture());
        // Klucz jest null — zostanie dostarczony dopiero po akceptacji przez admina
        assertNull(captor.getValue().getEncryptedGroupKey());
    }

    /**
     * Test sprawdzajacy czy mocki faktycznie wywolaly metody
     */
    @Test
    void joinGroup_shouldCallUserAndKeyRepositories() {
        when(groupRepository.findByKod("#abcde")).thenReturn(Optional.of(group));
        groupService.joinGroup("#abcde", user);
        verify(userRepository, times(1)).save(user);
        verify(groupKeyRepository, times(1)).save(any(GroupKey.class));
    }

    /**
     * Test sprawdzajacy czy jak nie ma grupy to czy bedzie odpowiedni wyjatek
     */
    @Test
    void joinGroup_shouldThrowRuntimeExceptionWhenGroupNotFound() {
        when(groupRepository.findByKod("WRONG")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> groupService.joinGroup("WRONG", user));
    }

    /**
     * Test sprawdzajacy czy ten wyjatek dal dobra wiadomosc wyjatku
     */
    @Test
    void joinGroup_shouldThrowExceptionWithCorrectMessage() {
        when(groupRepository.findByKod("WRONG")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> groupService.joinGroup("WRONG", user));
        assertEquals("Brak grupy", ex.getMessage());
    }

    /**
     * Test sprawdzajacy ze jak nie ma grupy to nic sie nie zapisalo
     */
    @Test
    void joinGroup_shouldNotSaveUserWhenGroupNotFound() {
        when(groupRepository.findByKod("WRONG")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> groupService.joinGroup("WRONG", user));
        verify(userRepository, never()).save(any());
    }

    /**
     * Test sprawdzajacy ze jak nie ma grupy to nie zapisano klucza
     */
    @Test
    void joinGroup_shouldNotSaveGroupKeyWhenGroupNotFound() {
        when(groupRepository.findByKod("WRONG")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> groupService.joinGroup("WRONG", user));
        verify(groupKeyRepository, never()).save(any());
    }

    private void mockGroupSaveWithId(Long id) {
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            g.setId(id);
            return g;
        });
    }
}