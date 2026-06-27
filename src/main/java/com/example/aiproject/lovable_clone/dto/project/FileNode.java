package com.example.aiproject.lovable_clone.dto.project;

import org.jspecify.annotations.NonNull;

import java.time.Instant;

public record FileNode(
        String path
) {
    @Override
    public @NonNull String toString() {
        return path;
    }
}
