package com.KryptoChat.serwer.services;
import com.KryptoChat.serwer.config.GroupCodeGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.KryptoChat.serwer.repositories.*;
import com.KryptoChat.serwer.entities.*;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long createGroup(String name, User user) {

        String code;
        do {
            code = GroupCodeGenerator.generateCode();
        } while (groupRepository.findByKod(code).isPresent());

        Group group = new Group(name, code);
        groupRepository.save(group);

        user.setGroup(group);
        userRepository.save(user);

        return group.getId();
    }

    @Transactional
    public Long joinGroup(String code, User user) {

        Group group = groupRepository.findByKod(code)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        user.setGroup(group);
        userRepository.save(user);

        return group.getId();
    }
}
