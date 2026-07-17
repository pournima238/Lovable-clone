package com.example.workspace_service.dto.member;


import com.example.common_lib.enums.ProjectRole;

import java.time.Instant;

public record MemberResponse(
        Long id,
        String username,
        String name,
        ProjectRole role,
        Instant invitedAt
) {

}
