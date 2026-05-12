package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.member.InviteMemberRequest;
import com.example.aiproject.lovable_clone.dto.member.MemberResponse;
import com.example.aiproject.lovable_clone.dto.member.UpdateMemberRoleRequest;
import com.example.aiproject.lovable_clone.entity.Project;
import com.example.aiproject.lovable_clone.entity.ProjectMember;
import com.example.aiproject.lovable_clone.entity.ProjectMemberId;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.enums.ProjectRole;
import com.example.aiproject.lovable_clone.mapper.ProjectMemberMapper;
import com.example.aiproject.lovable_clone.repository.ProjectMemberRepository;
import com.example.aiproject.lovable_clone.repository.ProjectRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
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
    UserRepository userRepository;

    @Override
    public List<MemberResponse> getProjectMembers(Long projectId) {
        //first find project
        Long userId = 1L;
        //calling this to find the owner
        Project project = findProjectById(projectId, userId);
        List<MemberResponse> memberResponseList = new ArrayList<>();
        memberResponseList.add(projectMemberMapper.toProjectMemberResponseFromUserOwner(project.getOwner()));
        memberResponseList.addAll(projectMemberRepository.findByIdProjectId(projectId).stream().map(member ->
                projectMemberMapper.toProjectMemberResponseFromProjectMember(member)).collect(Collectors.toList()));
        return memberResponseList;

    }

    @Override
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId) {
        Project project = findProjectById(projectId,userId);
        if(!project.getOwner().getId().equals(userId)){
            throw new RuntimeException("You are not owner of this project so cannot send the invite");
        }
        User user = userRepository.findByEmail(request.email());
        if(user.getId().equals(userId)){
            throw new RuntimeException("Owner cannot request itself");
        }
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId,userId);
       if(projectMemberRepository.existsById(projectMemberId)){
           throw new RuntimeException("This user is already part of this project");
       }
       ProjectMember projectMember = ProjectMember.builder().project(project).user(user).invitedAt(Instant.now()).projectRole(ProjectRole.VIEWER).id(projectMemberId).build();
       projectMember=projectMemberRepository.save(projectMember);
       return projectMemberMapper.toProjectMemberResponseFromProjectMember(projectMember);
    }

    @Override
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId) {
        return null;
    }

    @Override
    public MemberResponse deleteMemberRole(Long projectId, Long memberId, Long userId) {
        return null;
    }

    public Project findProjectById(Long projectId, Long userId) {
        return projectRepository.findAllAccessibleByProject(userId, projectId).orElseThrow();
    }
}
