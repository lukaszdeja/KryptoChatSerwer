package com.KryptoChat.serwer.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.*;
import com.KryptoChat.serwer.entities.*;
import com.KryptoChat.serwer.repositories.GroupRepository;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final GroupRepository groupRepository;

    public GroupController(GroupService groupService, UserService userService, GroupRepository groupRepository) {
        this.groupService = groupService;
        this.userService = userService;
        this.groupRepository = groupRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<GroupResponse> createGroup(@RequestBody CreateGroupRequest request) {

        User user = userService.findByUsername(request.getUsername());

        Long groupId = groupService.createGroup(request.getGroupName(), user);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        return ResponseEntity.ok(new GroupResponse(groupId, group.getKod(), "Utworzono grupę"));
    }

    @PostMapping("/join")
    public ResponseEntity<GroupResponse> joinGroup(@RequestBody JoinGroupRequest request) {

        User user = userService.findByUsername(request.getUsername());

        Long groupId = groupService.joinGroup(request.getCode(), user);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        return ResponseEntity.ok(new GroupResponse(groupId, group.getKod(), "Dołączono do grupy"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDetailsResponse> getGroup(@PathVariable Long id) {

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        List<UserResponse> users = group.getUsers()
                .stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername()))
                .toList();

        GroupDetailsResponse response = new GroupDetailsResponse(group.getId(), group.getGroupName(), group.getKod(), users);

        return ResponseEntity.ok(response);
    }
}