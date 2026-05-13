package com.example.aiproject.lovable_clone.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest (
       @NotBlank @Email String email,
        @Size(min=4) String password,
        String name
){
}
