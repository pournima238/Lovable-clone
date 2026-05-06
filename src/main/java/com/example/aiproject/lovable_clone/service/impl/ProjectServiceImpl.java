package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.project.ProjectRequest;
import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.entity.Project;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.repository.ProjectRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.service.ProjectService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class ProjectServiceImpl implements ProjectService {
    ProjectRepository projectRepository;
    UserRepository userRepository;
    @Override
    public List<ProjectSummaryResponse> getAllProjects(Long userId) {
        return null;
    }

//    @Override
//    public ProjectResponse getProjectById(Long id, Long userId) {
//        return null;
//    }

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {

        User owner=userRepository.findById(userId).orElseThrow();
        Project project=Project.builder().name(request.name()).owner(owner).build();
        project=projectRepository.save(project);
        return project;
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        return null;
    }

    @Override
    public void softDelete(Long id, Long userId) {

    }
}
