package com.example.account_service.services;


import com.example.account_service.dto.auth.AuthResponse;
import com.example.account_service.dto.auth.LoginRequest;
import com.example.account_service.dto.auth.SignUpRequest;

public interface AuthService {
    AuthResponse signUp(SignUpRequest request);

    AuthResponse login(LoginRequest request);
}
