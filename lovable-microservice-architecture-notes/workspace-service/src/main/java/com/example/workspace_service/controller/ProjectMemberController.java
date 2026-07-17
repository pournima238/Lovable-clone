package com.example.workspace_service.controller;

import com.example.common_lib.security.AuthUtil;
import com.example.workspace_service.dto.member.InviteMemberRequest;
import com.example.workspace_service.dto.member.MemberResponse;
import com.example.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.example.workspace_service.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;
    private final AuthUtil auth;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getProjectMembers(@PathVariable Long projectId) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectMemberService.getProjectMembers(projectId));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> inviteMember(
            @PathVariable Long projectId,
            @RequestBody @Valid InviteMemberRequest request
    ) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                projectMemberService.inviteMember(projectId, request, userId)
        );
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(@PathVariable Long projectId,
                                                           @PathVariable Long memberId,
                                                           @RequestBody @Valid UpdateMemberRoleRequest request) {

        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(projectMemberService.updateMemberRole(projectId, memberId, request, userId));
    }


    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteProjectMember(@PathVariable Long projectId,
                                                    @PathVariable Long memberId) {
        Long userId = auth.getCurrentUserId();
        projectMemberService.deleteMemberRole(projectId, memberId, userId);
        return ResponseEntity.noContent().build();
    }

}
