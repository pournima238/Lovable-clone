package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.member.InviteMemberRequest;
import com.example.aiproject.lovable_clone.dto.member.MemberResponse;
import com.example.aiproject.lovable_clone.dto.member.UpdateMemberRoleRequest;
import lombok.experimental.FieldDefaults;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;


public interface ProjectMemberService {
    List<MemberResponse> getProjectMembers(Long projectId);

    MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId);

     MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId);

    MemberResponse deleteMemberRole(Long projectId, Long memberId, Long userId);
}
