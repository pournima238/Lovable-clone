package com.example.workspace_service.dto.project;


import com.example.common_lib.enums.ProjectRole;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        ProjectRole role
) {
}
