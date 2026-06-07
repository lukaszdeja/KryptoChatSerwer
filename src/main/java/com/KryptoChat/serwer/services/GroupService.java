package com.KryptoChat.serwer.services;
import com.KryptoChat.serwer.config.GroupCodeGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.KryptoChat.serwer.repositories.*;
import com.KryptoChat.serwer.entities.*;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


/**
 * Serwis odpowiedzialny za zarządzanie grupami w systemie.
 * Obsługuje tworzenie nowych grup, dołączanie użytkowników
 * oraz inicjalizację i zarządzanie kluczami grupowymi
 * wykorzystywanymi do szyfrowania komunikacji.
 */
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupKeyRepository groupKeyRepository;


    /**
     * Konstruktor inicjujący serwis grup.
     *
     * @param groupRepository repozytorium grup
     * @param userRepository repozytorium użytkowników
     * @param groupkey repozytorium kluczy grupowych
     */

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupKeyRepository groupkey) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupKeyRepository = groupkey;
    }


    /**
     * Metoda tworząca nową grupę oraz przypisująca do niej użytkownika tworzącego.
     * Generuje unikalny kod dołączenia do grupy oraz inicjalizuje klucz grupowy dla użytkownika tworzącego grupę.
     *
     * @param groupName nazwa tworzonej grupy
     * @param creator użytkownik tworzący grupę
     * @param encryptedCreatorKey zaszyfrowany klucz grupowy dla twórcy
     * @return identyfikator utworzonej grupy
     */
    @Transactional
    public Long createGroup(String groupName, User creator, String encryptedCreatorKey) {

        Group group = new Group();
        group.setGroupName(groupName);
        group.setKod(GroupCodeGenerator.generateCode());

        groupRepository.save(group);

        List<User> users = List.of(creator);

        for (User user : users) {

            GroupKey gk = new GroupKey(group.getId(), user.getId(), encryptedCreatorKey);

            gk.setStatus("ACTIVE");
            groupKeyRepository.save(gk);
        }

        creator.setGroup(group);
        userRepository.save(creator);

        return group.getId();
    }

    /**
     * Metoda umożliwiająca dołączenie użytkownika do istniejącej grupy.
     * Weryfikuje kod grupy, przypisuje użytkownika do grupy oraz
     * tworzy wpis GroupKey w statusie PENDING oczekujący na przekazanie klucza.
     *
     * @param code kod dołączenia do grupy
     * @param user użytkownik dołączający do grupy
     * @return identyfikator grupy, do której dołączono użytkownika
     */
    @Transactional
    public Long joinGroup(String code, User user) {

        Group group = groupRepository.findByKod(code)
                .orElseThrow(() -> new RuntimeException("Brak grupy"));

        user.setGroup(group);
        userRepository.save(user);

        GroupKey gk = new GroupKey(group.getId(), user.getId(), null);

        gk.setStatus("PENDING");

        groupKeyRepository.save(gk);

        return group.getId();
    }

}
