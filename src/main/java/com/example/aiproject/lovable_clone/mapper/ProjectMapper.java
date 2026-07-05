package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.entity.Project;
import com.example.aiproject.lovable_clone.enums.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")//since want this map struct library to work for sb project
public interface ProjectMapper {
    @Mapping(target = "id", source = "project.id")
    @Mapping(target = "name", source = "project.name")
    @Mapping(target = "createdAt", source = "project.createdAt")
    @Mapping(target = "updatedAt", source = "project.updatedAt")
    @Mapping(target = "role", source = "role")
    ProjectResponse toProjectResponse(Project project, ProjectRole role);

    //    @Mapping(source = "name", target = "projectName")
    ProjectSummaryResponse toProjectSummaryResponse(Project project);

    //    @Mapping(source = "name", target = "projectName")
    List<ProjectSummaryResponse> toProjectSummaryResponseList(List<Project> projects);
}
