package com.example.aiproject.lovable_clone.controller;

import com.example.aiproject.lovable_clone.dto.project.FileContentResponse;
import com.example.aiproject.lovable_clone.dto.project.FileNode;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.FileService;
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
    private final FileService fileService;
    private AuthUtil auth;

    @GetMapping
    public ResponseEntity<List<FileNode>> getFileTree(@PathVariable Long projectId){
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(fileService.getFileTree(projectId,userId));
    }

    @GetMapping("/{*path}") //src/hooks/.jsx
    public ResponseEntity<FileContentResponse>getFile(
        @PathVariable Long projectId,
                @PathVariable String path
    ){
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(fileService.getFileContent(projectId,path, userId));
    }
}
