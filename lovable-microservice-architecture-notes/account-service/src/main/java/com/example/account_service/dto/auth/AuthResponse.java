package com.example.account_service.dto.auth;

public record AuthResponse(String token, UserProfileResponse user) {
}
