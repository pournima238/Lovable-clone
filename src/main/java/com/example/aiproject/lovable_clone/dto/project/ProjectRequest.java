package com.example.aiproject.lovable_clone.dto.project;

import jakarta.validation.constraints.Size;

public record ProjectRequest(@Size(min=1, max = 50) String name) {
}
