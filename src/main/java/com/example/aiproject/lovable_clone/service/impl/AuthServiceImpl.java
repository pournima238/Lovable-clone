package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.auth.AuthResponse;
import com.example.aiproject.lovable_clone.dto.auth.LoginRequest;
import com.example.aiproject.lovable_clone.dto.auth.SignUpRequest;
import com.example.aiproject.lovable_clone.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @Override
    public AuthResponse signUp(SignUpRequest request) {
        return null;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return null;
    }
}
