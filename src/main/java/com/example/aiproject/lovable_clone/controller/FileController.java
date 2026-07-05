package com.example.aiproject.lovable_clone.controller;

import com.example.aiproject.lovable_clone.dto.project.FileContentResponse;
import com.example.aiproject.lovable_clone.dto.project.FileNode;
import com.example.aiproject.lovable_clone.dto.project.FileTreeResponse;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
