package com.example.workspace_service.dto.project;

import java.util.List;

public record FileTreeResponse(
        List<FileNode> files
) {
}
