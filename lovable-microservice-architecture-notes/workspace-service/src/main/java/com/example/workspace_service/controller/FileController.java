package com.example.workspace_service.controller;

import com.example.common_lib.security.AuthUtil;
import com.example.workspace_service.dto.project.FileContentResponse;
import com.example.workspace_service.dto.project.FileTreeResponse;
import com.example.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/files")
public class FileController {
    private final ProjectFileService projectFileService;
    private final AuthUtil auth;

    @GetMapping
    public ResponseEntity<FileTreeResponse> getFileTree(@PathVariable Long projectId) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectFileService.getFileTree(projectId));
    }

    @GetMapping("/content")
    public ResponseEntity<FileContentResponse> getFile(
            @PathVariable Long projectId,
            @org.springframework.web.bind.annotation.RequestParam("path") String path
    ) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectFileService.getFileContent(projectId, path));
    }
}
