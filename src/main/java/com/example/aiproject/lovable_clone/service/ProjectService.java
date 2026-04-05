package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.project.ProjectRequest;
import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getAllProjects(Long userId);

    ProjectResponse getProjectById(Long id,Long userId);

    ProjectResponse createProject(ProjectRequest request, Long userId);

    ProjectResponse updateProject(Long id, ProjectRequest request, Long userId);

    void softDelete(Long id, Long userId);
}
