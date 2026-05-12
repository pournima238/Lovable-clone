package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.member.MemberResponse;
import com.example.aiproject.lovable_clone.entity.ProjectMember;
import com.example.aiproject.lovable_clone.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")//since want this map struct library to work for sb project
public interface ProjectMemberMapper {
    @Mapping(target="role", constant = "OWNER")
    MemberResponse toProjectMemberResponseFromUserOwner(User owner);

    @Mapping(target = "id",        source = "user.id")
    MemberResponse toProjectMemberResponseFromProjectMember(ProjectMember member);
}
