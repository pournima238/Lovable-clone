package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.member.MemberResponse;
import com.example.aiproject.lovable_clone.entity.ProjectMember;
import com.example.aiproject.lovable_clone.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "role",      constant = "OWNER")
    @Mapping(target = "invitedAt", ignore = true)   // User has no invitedAt
    MemberResponse toProjectMemberResponseFromUserOwner(User owner);

    @Mapping(target = "id",        source = "user.id")
    @Mapping(target = "username",     source = "user.username")
    @Mapping(target = "name",      source = "user.name")
    @Mapping(target = "role",      source = "projectRole")  // name mismatch
    MemberResponse toProjectMemberResponseFromProjectMember(ProjectMember member);
}
