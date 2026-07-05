package com.example.aiproject.lovable_clone.dto.project;

import com.example.aiproject.lovable_clone.dto.auth.UserProfileResponse;

import com.example.aiproject.lovable_clone.enums.ProjectRole;
import java.time.Instant;

public record ProjectResponse (
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        ProjectRole role
){
}
