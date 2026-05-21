package com.example.aiproject.lovable_clone.dto.member;

import com.example.aiproject.lovable_clone.enums.ProjectRole;

import java.time.Instant;

public record MemberResponse(
        Long id,
        String username,
        String name,
        ProjectRole role,
        Instant invitedAt
) {

}
