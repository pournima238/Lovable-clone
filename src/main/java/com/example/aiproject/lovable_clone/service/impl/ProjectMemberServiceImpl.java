package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.member.InviteMemberRequest;
import com.example.aiproject.lovable_clone.dto.member.MemberResponse;
import com.example.aiproject.lovable_clone.dto.member.UpdateMemberRoleRequest;
import com.example.aiproject.lovable_clone.service.ProjectMemberService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectMemberServiceImpl implements ProjectMemberService {
    @Override
    public List<MemberResponse> getProjectMembers(Long projectId) {
        return null;
    }

    @Override
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId) {
        return null;
    }

    @Override
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId) {
        return null;
    }

    @Override
    public MemberResponse deleteMemberRole(Long projectId, Long memberId, Long userId) {
        return null;
    }
}
