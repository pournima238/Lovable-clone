package com.example.intelligence_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "workspace-service", path = "/workspace")
public interface WorkspaceClient {

    @GetMapping("/api/projects/{projectId}/files")
    FileTreeResponse getFileTree(@PathVariable("projectId") Long projectId);

    @GetMapping("/api/projects/{projectId}/files/content")
    FileContentResponse getFileContent(@PathVariable("projectId") Long projectId, @RequestParam("path") String path);

    @PostMapping("/api/projects/{projectId}/files")
    void saveFile(@PathVariable("projectId") Long projectId, @RequestBody SaveFileRequest request);

    @GetMapping("/api/projects/{projectId}")
    ProjectSummaryResponse getProject(@PathVariable("projectId") Long projectId);

    record FileTreeResponse(List<FileNode> files) {}
    record FileNode(String name, String type, String path, List<FileNode> children) {}
    record FileContentResponse(String content) {}
    record SaveFileRequest(String path, String content) {}
    record ProjectSummaryResponse(Long id, String name) {}
}
