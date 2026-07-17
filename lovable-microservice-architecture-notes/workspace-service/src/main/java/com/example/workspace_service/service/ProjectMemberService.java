package com.example.workspace_service.service;


import com.example.workspace_service.dto.member.InviteMemberRequest;
import com.example.workspace_service.dto.member.MemberResponse;
import com.example.workspace_service.dto.member.UpdateMemberRoleRequest;

import java.util.List;


public interface ProjectMemberService {
    List<MemberResponse> getProjectMembers(Long projectId);

    MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId);

    MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId);

    void deleteMemberRole(Long projectId, Long memberId, Long userId);
}
