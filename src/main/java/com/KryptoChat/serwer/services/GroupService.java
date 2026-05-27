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

import com.KryptoChat.serwer.security.RSAEncryptionUtil;

import static com.KryptoChat.serwer.security.RSAEncryptionUtil.encryptRSA;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupKeyRepository groupKeyRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupKeyRepository groupkey) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupKeyRepository = groupkey;
    }

    @Transactional
    public Long createGroup(String groupName, User creator) {

        Group group = new Group();
        group.setGroupName(groupName);
        group.setKod(UUID.randomUUID().toString().substring(0, 8));

        groupRepository.save(group);

        String aesKey = generateAESKey();

        List<User> users = List.of(creator);

        for (User user : users) {

            String encryptedKey = encryptRSA(aesKey, user.getPublicKey());

            GroupKey gk = new GroupKey(group.getId(), user.getId(), encryptedKey);

            groupKeyRepository.save(gk);
        }

        creator.setGroup(group);
        userRepository.save(creator);

        return group.getId();
    }

    @Transactional
    public Long joinGroup(String code, User user) {

        Group group = groupRepository.findByKod(code)
                .orElseThrow(() -> new RuntimeException("Brak grupy"));

        user.setGroup(group);
        userRepository.save(user);

        String aesKey = getAESKeyForGroup(group.getId());

        String encrypted = encryptRSA(aesKey, user.getPublicKey());

        GroupKey gk = new GroupKey(group.getId(), user.getId(), encrypted);

        groupKeyRepository.save(gk);

        return group.getId();
    }

    public String generateAESKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);

            SecretKey key = generator.generateKey();

            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getAESKeyForGroup(Long groupId) {
        // tu cos ma byc
        throw new RuntimeException("TODO: store group AES key securely");
    }
}
