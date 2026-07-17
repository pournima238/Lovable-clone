package com.example.workspace_service.dto.project;

import jakarta.validation.constraints.Size;

public record ProjectRequest(@Size(min = 1, max = 50) String name) {
}
