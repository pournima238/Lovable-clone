package com.example.account_service.controller;

import com.example.account_service.dto.auth.AuthResponse;
import com.example.account_service.dto.auth.LoginRequest;
import com.example.account_service.dto.auth.SignUpRequest;
import com.example.account_service.dto.auth.UserProfileResponse;
import com.example.account_service.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
//    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignUpRequest request) {
        return ResponseEntity.ok(authService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

//    @GetMapping("/me")
//    public ResponseEntity<UserProfileResponse> getProfile() {
//        Long userId = 1L; //later this can fetched using context in spring security
//        return ResponseEntity.ok(userService.getProfile(userId));
//    }
}
