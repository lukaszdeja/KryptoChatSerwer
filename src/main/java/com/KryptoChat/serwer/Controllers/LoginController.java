package com.KryptoChat.serwer.Controllers;
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

        User user = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUsername(),
                "Zalogowano"
        );

        return ResponseEntity.ok(response);
    }
}