package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.project.ProjectRequest;
import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.entity.Project;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.mapper.ProjectMapper;
import com.example.aiproject.lovable_clone.repository.ProjectRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.service.ProjectService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE) //no need to write private final because of this
@Transactional
public class ProjectServiceImpl implements ProjectService {
    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMapper projectMapper;
    @Override
    public List<ProjectSummaryResponse> getAllProjects(Long userId) {
//        return projectRepository.findAllAccessibleByUser(userId).stream().map(project->projectMapper.toProjectSummaryResponse(project)).collect(Collectors.toList());
    List<Project>allProjects = projectRepository.findAllAccessibleByUser(userId);
    return projectMapper.toProjectSummaryResponseList(allProjects);
    }

    @Override
    public ProjectResponse getProjectById(Long id, Long userId) {
        Project project= projectRepository.findAllAccessibleByProject(userId,id).orElseThrow();
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {

        User owner=userRepository.findById(userId).orElseThrow();
        Project project=Project.builder().name(request.name()).owner(owner).isPublic(false).build();
        project=projectRepository.save(project);
        ProjectResponse savedProject = projectMapper.toProjectResponse(project);
        return savedProject;
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {

        Project project = projectRepository.findAllAccessibleByProject(userId,id).orElseThrow();
        project.setName(request.name());
        project = this.projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public void softDelete(Long id, Long userId) {
        Project project = projectRepository.findAllAccessibleByProject(userId,id).orElseThrow();
        if(!project.getOwner().getId().equals(userId)){
            throw new RuntimeException("You are not owner so you cannot delete this project");
        }
        project.setDeletedAt(Instant.now());
//        this.projectRepository.save(project);// since we are using transactional it has become dirty so need to write this
    }
}
