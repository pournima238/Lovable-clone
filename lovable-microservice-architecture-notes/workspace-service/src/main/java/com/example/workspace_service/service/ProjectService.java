package com.example.workspace_service.service;


import com.example.workspace_service.dto.project.ProjectRequest;
import com.example.workspace_service.dto.project.ProjectResponse;
import com.example.workspace_service.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getAllProjects(Long userId);

    ProjectResponse getProjectById(Long id, Long userId);

    ProjectResponse createProject(ProjectRequest request, Long userId);

    ProjectResponse updateProject(Long id, ProjectRequest request, Long userId);

    void softDelete(Long id, Long userId);
}
