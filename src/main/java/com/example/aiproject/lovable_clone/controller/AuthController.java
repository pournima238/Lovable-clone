package com.example.aiproject.lovable_clone.controller;

import com.example.aiproject.lovable_clone.dto.auth.AuthResponse;
import com.example.aiproject.lovable_clone.dto.auth.LoginRequest;
import com.example.aiproject.lovable_clone.dto.auth.SignUpRequest;
import com.example.aiproject.lovable_clone.dto.auth.UserProfileResponse;
import com.example.aiproject.lovable_clone.service.AuthService;
import com.example.aiproject.lovable_clone.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(SignUpRequest request){
        return ResponseEntity.ok(authService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse>login(LoginRequest request){
     return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(){
        Long userId=1L; //later this can fetched using context in spring security
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
