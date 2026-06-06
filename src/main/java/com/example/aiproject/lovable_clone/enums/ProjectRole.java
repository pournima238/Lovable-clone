package com.example.aiproject.lovable_clone.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static com.example.aiproject.lovable_clone.enums.ProjectPermission.*;

@RequiredArgsConstructor
@Getter
public enum ProjectRole {
    EDITOR(Set.of(VIEW,EDIT,DELETE)),
    VIEWER(Set.of(ProjectPermission.VIEW,VIEW_MEMBERS)),
    OWNER (Set.of(ProjectPermission.VIEW,ProjectPermission.MANAGE_MEMBERS,VIEW_MEMBERS,EDIT,DELETE));

    private final Set<ProjectPermission> permissions;
}
