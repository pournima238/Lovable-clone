package com.example.workspace_service.service.impl;

import com.example.common_lib.dto.UserDto;
import com.example.common_lib.error.ResourceNotFoundException;
import com.example.common_lib.security.AuthUtil;
import com.example.workspace_service.client.AccountClient;
import com.example.workspace_service.dto.member.InviteMemberRequest;
import com.example.workspace_service.dto.member.MemberResponse;
import com.example.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.example.workspace_service.entity.Project;
import com.example.workspace_service.entity.ProjectMember;
import com.example.workspace_service.entity.ProjectMemberId;
import com.example.workspace_service.mapper.ProjectMemberMapper;
import com.example.workspace_service.repository.ProjectMemberRepository;
import com.example.workspace_service.repository.ProjectRepository;
import com.example.workspace_service.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE) //no need to write private final because of this
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {
    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;

    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        return projectMemberRepository.findByIdProjectId(projectId).stream()
                .map(member -> {
                    UserDto user = accountClient.getUserById(member.getId().getUserId());
                    return projectMemberMapper.toMemberResponse(member, user);
                })
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId) {
        Project project = getAccessibleProjectById(projectId, userId);

        UserDto invitee = accountClient.getUserByEmail(request.username()).orElseThrow(
                () -> new ResourceNotFoundException("User", request.username())
        );

        if (invitee.id().equals(userId)) {
            throw new RuntimeException("Cannot invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.id());

        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Cannot invite once again");
        }

        ProjectMember member = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();

        projectMemberRepository.save(member);

        return projectMemberMapper.toMemberResponse(member, invitee);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId) {
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setProjectRole(request.role());

        projectMember = projectMemberRepository.save(projectMember);
        UserDto user = accountClient.getUserById(memberId);
        return projectMemberMapper.toMemberResponse(projectMember, user);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void deleteMemberRole(Long projectId, Long memberId, Long userId) {
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        projectMemberRepository.deleteById(projectMemberId);
    }

    private Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }
}
