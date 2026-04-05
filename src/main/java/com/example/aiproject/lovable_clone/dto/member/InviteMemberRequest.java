package com.example.aiproject.lovable_clone.dto.member;

import com.example.aiproject.lovable_clone.enums.ProjectRole;

public record InviteMemberRequest(
     String email,
     ProjectRole role
) {
}
