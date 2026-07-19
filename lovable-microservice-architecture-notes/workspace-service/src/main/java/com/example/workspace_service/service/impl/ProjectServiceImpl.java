package com.example.workspace_service.service.impl;

import com.example.common_lib.dto.PlanDto;
import com.example.common_lib.enums.ProjectPermission;
import com.example.common_lib.enums.ProjectRole;
import com.example.common_lib.error.BadRequestException;
import com.example.common_lib.error.ResourceNotFoundException;
import com.example.common_lib.security.AuthUtil;
import com.example.workspace_service.client.AccountClient;
import com.example.workspace_service.dto.project.ProjectRequest;
import com.example.workspace_service.dto.project.ProjectResponse;
import com.example.workspace_service.dto.project.ProjectSummaryResponse;
import com.example.workspace_service.entity.Project;
import com.example.workspace_service.entity.ProjectMember;
import com.example.workspace_service.entity.ProjectMemberId;
import com.example.workspace_service.mapper.ProjectMapper;
import com.example.workspace_service.repository.ProjectMemberRepository;
import com.example.workspace_service.repository.ProjectRepository;
import com.example.workspace_service.security.SecurityExpressions;
import com.example.workspace_service.service.ProjectService;
import com.example.workspace_service.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions;

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {

        if (!canCreateProject(userId)) {
            throw new BadRequestException("User cannot create a New project with current Plan, Upgrade plan now.");
        }

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();
        project = projectRepository.save(project);

        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), userId);
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();
        projectMemberRepository.save(projectMember);

        projectTemplateService.initializeProjectFromTemplate(project.getId());

        return projectMapper.toProjectResponse(project, ProjectRole.OWNER);
    }

    @Override
    public List<ProjectSummaryResponse> getAllProjects(Long userId) {
        var projectsWithRoles = projectRepository.findAllAccessibleByUser(userId);
        return projectsWithRoles.stream()
                .map(p -> projectMapper.toProjectSummaryResponse(p.getProject(), p.getRole()))
                .toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#id)")
    public ProjectResponse getProjectById(Long id, Long userId) {
        var projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(id, userId)
                .orElseThrow(() -> new BadRequestException("Project Not Found"));

        return projectMapper.toProjectResponse(projectWithRole.getProject(), projectWithRole.getRole());
    }

    @Override
    @PreAuthorize("@security.canEditProject(#id)")
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = getAccessibleProjectById(id, userId);

        project.setName(request.name());
        project = projectRepository.save(project);

        ProjectRole role = projectMemberRepository.findRoleByProjectIdAndUserId(id, userId)
                .orElse(ProjectRole.VIEWER);

        return projectMapper.toProjectResponse(project, role);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#id)")
    public void softDelete(Long id, Long userId) {
        Project project = getAccessibleProjectById(id, userId);

        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    public boolean hasPermission(Long projectId, ProjectPermission permission) {
        return securityExpressions.hasPermission(projectId, permission);
    }

    ///  INTERNAL FUNCTIONS

    public Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }

    private boolean canCreateProject(Long userId) {
        if (userId == null) {
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();

        int maxAllowed = (plan != null) ? plan.maxProjects() : 100;
        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);

        return ownedCount < maxAllowed;
    }
}
