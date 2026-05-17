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
    public ResponseEntity<GroupResponse> createGroup(@RequestHeader("Authorization") String header, @RequestBody CreateGroupRequest request) {

        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);
        JWTService jwtService = new JWTService();
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);

        Long groupId = groupService.createGroup(request.getGroupName(), user);
        String newToken = null;
        String message = "Nie udalo sie utworzyc grupy";
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));

        if (group != null) {
            newToken = jwtService.generateToken(user);
            message = "Uwtorzono grupe";
        }

        return ResponseEntity.ok(new GroupResponse(newToken, new UserCredentials(user.getId(), user.getUsername(), user.getGroup().getId()), message));
    }

    @PostMapping("/join")
    public ResponseEntity<GroupResponse> joinGroup(@RequestHeader("Authorization") String header, @RequestBody JoinGroupRequest request) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = header.substring(7);
        JWTService jwtService = new JWTService();
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(token);

        User user = userService.authentification(userId);

        Long groupId = groupService.joinGroup(request.getCode(), user);
        String newToken = null;
        String message = "Nie udalo sie utworzyc grupy";
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono grupy"));
        if (group != null) {
            newToken = jwtService.generateToken(user);
            message = "Dolaczono do grupy";
        }

        return ResponseEntity.ok(new GroupResponse(newToken, new UserCredentials(user.getId(), user.getUsername(), user.getGroup().getId()), message));
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