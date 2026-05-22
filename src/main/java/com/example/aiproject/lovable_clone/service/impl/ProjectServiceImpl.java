package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.project.ProjectRequest;
import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.entity.Project;
import com.example.aiproject.lovable_clone.entity.ProjectMember;
import com.example.aiproject.lovable_clone.entity.ProjectMemberId;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.enums.ProjectRole;
import com.example.aiproject.lovable_clone.error.ResourceNotFoundException;
import com.example.aiproject.lovable_clone.mapper.ProjectMapper;
import com.example.aiproject.lovable_clone.repository.ProjectMemberRepository;
import com.example.aiproject.lovable_clone.repository.ProjectRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.security.AuthUtil;
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
    ProjectMemberRepository projectMemberRepository;
    @Override
    public List<ProjectSummaryResponse> getAllProjects(Long userId) {
    List<Project>allProjects = projectRepository.findAllAccessibleByUser(userId);
    return projectMapper.toProjectSummaryResponseList(allProjects);
    }

    @Override
    public ProjectResponse getProjectById(Long id, Long userId) {
        Project project= projectRepository.findAllAccessibleByProject(userId,id).orElseThrow(()->new ResourceNotFoundException("Project",id.toString()));
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {
//        User owner = userRepository.findById(userId)
//                .orElseThrow();
        User owner= userRepository.getReferenceById(userId);

        // Create project
        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        // SAVE PROJECT FIRST
        project = projectRepository.save(project);

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
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {

        Project project = projectRepository.findAllAccessibleByProject(userId,id).orElseThrow((()->new ResourceNotFoundException("Project",id.toString())));
        project.setName(request.name());
        project = this.projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public void softDelete(Long id, Long userId) {
        Project project = projectRepository.findAllAccessibleByProject(userId,id).orElseThrow((()->new ResourceNotFoundException("Project",id.toString())));
        project.setDeletedAt(Instant.now());
//        this.projectRepository.save(project);// since we are using transactional it has become dirty so need to write this
    }
}
