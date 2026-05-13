package com.example.aiproject.lovable_clone.dto.member;

import com.example.aiproject.lovable_clone.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
       @NotNull  ProjectRole role
) {
}
