package com.example.workspace_service.mapper;

import com.example.common_lib.dto.UserDto;
import com.example.workspace_service.dto.member.MemberResponse;
import com.example.workspace_service.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "id", source = "projectMember.id.userId")
    @Mapping(target = "role", source = "projectMember.projectRole")
    @Mapping(target = "username", source = "userDto.userName")
    @Mapping(target = "name", source = "userDto.name")
    @Mapping(target = "invitedAt", source = "projectMember.invitedAt")
    MemberResponse toMemberResponse(ProjectMember projectMember, UserDto userDto);
}
