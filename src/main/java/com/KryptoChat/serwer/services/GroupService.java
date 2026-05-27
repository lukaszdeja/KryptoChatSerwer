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
