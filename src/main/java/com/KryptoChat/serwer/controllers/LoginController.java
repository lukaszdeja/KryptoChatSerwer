package com.KryptoChat.serwer.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.KryptoChat.serwer.services.*;
import com.KryptoChat.serwer.entities.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody RegisterRequest request) {
        User user;
        LoginResponse response;
            user = userService.login(
                    request.getUsername(),
                    request.getPassword()
            );
            UserToken token = new UserToken(
                    user.getId(),
                    user.getUsername(),
                    user.getGroupId()
            );

            response = new LoginResponse(
                    token,
                    "Zalogowano"
            );
            System.out.println(response.getUserToken().getUsername());
            

        return ResponseEntity.ok(response);
    }
}