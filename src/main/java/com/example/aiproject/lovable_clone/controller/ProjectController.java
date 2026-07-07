package com.example.aiproject.lovable_clone.controller;

import com.example.aiproject.lovable_clone.dto.deploy.DeployResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectRequest;
import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.DeploymentService;
import com.example.aiproject.lovable_clone.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final AuthUtil auth;
    private final DeploymentService deploymentService;

    @GetMapping()
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects() {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectService.getAllProjects(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProjectById(id, userId));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id, @RequestBody @Valid ProjectRequest request) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectService.updateProject(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        Long userId = auth.getCurrentUserId();
        projectService.softDelete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeployResponse> deployProject(@PathVariable Long id) {
        return ResponseEntity.ok(deploymentService.deploy(id));
    }

}
