package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.project.ProjectRequest;
import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.entity.Project;
import com.example.aiproject.lovable_clone.entity.ProjectMember;
import com.example.aiproject.lovable_clone.entity.ProjectMemberId;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.enums.ProjectRole;
import com.example.aiproject.lovable_clone.error.BadRequestException;
import com.example.aiproject.lovable_clone.error.ResourceNotFoundException;
import com.example.aiproject.lovable_clone.mapper.ProjectMapper;
import com.example.aiproject.lovable_clone.repository.ProjectMemberRepository;
import com.example.aiproject.lovable_clone.repository.ProjectRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.ProjectService;
import com.example.aiproject.lovable_clone.service.ProjectTemplateService;
import com.example.aiproject.lovable_clone.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE) //no need to write private final because of this
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {
    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    SubscriptionService subscriptionService;
    ProjectTemplateService projectTemplateService;

    @Override
    public List<ProjectSummaryResponse> getAllProjects(Long userId) {
        List<Project> allProjects = projectRepository.findAllAccessibleByUser(userId);
        return projectMapper.toProjectSummaryResponseList(allProjects);
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")// this is spring expression language not compiled by java
    public ProjectResponse getProjectById(Long projectId, Long userId) {
        Project project = projectRepository.findAllAccessibleByProject(userId, projectId).orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {
        User owner = userRepository.getReferenceById(userId);

        if (!subscriptionService.canCreateNewProject()) {

            log.debug("cannot create a project");
            throw new BadRequestException("User cannot create a new project with a current plan, Upgrade plan now");
        }
        // Create project
        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        // SAVE PROJECT FIRST
        project = projectRepository.save(project);

        //to save starter project template in minio
        projectTemplateService.initializeProjectFromTemplate(project.getId());

        // NOW project has generated ID
        ProjectMemberId projectMemberId =
                new ProjectMemberId(project.getId(), userId);

        // Create member
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .user(owner)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .build();

        // Save membership
        projectMemberRepository.save(projectMember);

        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, Long userId) {

        Project project = projectRepository.findAllAccessibleByProject(userId, projectId).orElseThrow((() -> new ResourceNotFoundException("Project", projectId.toString())));
        project.setName(request.name());
        project = this.projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void softDelete(Long projectId, Long userId) {
        Project project = projectRepository.findAllAccessibleByProject(userId, projectId).orElseThrow((() -> new ResourceNotFoundException("Project", projectId.toString())));
        project.setDeletedAt(Instant.now());
//        this.projectRepository.save(project);// since we are using transactional it has become dirty so need to write this
    }
}
