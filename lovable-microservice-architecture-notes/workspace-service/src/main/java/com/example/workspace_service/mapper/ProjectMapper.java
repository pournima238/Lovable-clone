package com.example.workspace_service.mapper;

import com.example.common_lib.enums.ProjectRole;
import com.example.workspace_service.dto.project.ProjectResponse;
import com.example.workspace_service.dto.project.ProjectSummaryResponse;
import com.example.workspace_service.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponse toProjectResponse(Project project);

    ProjectResponse toProjectResponse(Project project, ProjectRole role);

    ProjectSummaryResponse toProjectSummaryResponse(Project project, ProjectRole role);

    List<ProjectSummaryResponse> toListOfProjectSummaryResponse(List<Project> projects);

}
