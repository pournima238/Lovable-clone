package com.example.aiproject.lovable_clone.dto.auth;

public record SignUpRequest (
        String email,
        String password,
        String name
){
}
