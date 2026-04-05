package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.auth.AuthResponse;
import com.example.aiproject.lovable_clone.dto.auth.LoginRequest;
import com.example.aiproject.lovable_clone.dto.auth.SignUpRequest;

public interface AuthService {
    AuthResponse signUp(SignUpRequest request);

    AuthResponse login(LoginRequest request);
}
