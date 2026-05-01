package com.KryptoChat.serwer.Controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class RegisterController {

    @PostMapping("/register")
    public ResponseEntity<String> login(@RequestBody RegisterRequest request) {
        System.out.println("LOGIN: " + request.getUsername());

        return ResponseEntity.ok("OK");
    }
}
