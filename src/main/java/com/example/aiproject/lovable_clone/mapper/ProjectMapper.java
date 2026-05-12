package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.project.ProjectResponse;
import com.example.aiproject.lovable_clone.dto.project.ProjectSummaryResponse;
import com.example.aiproject.lovable_clone.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel="spring")//since want this map struct library to work for sb project
public interface ProjectMapper {
    ProjectResponse toProjectResponse(Project project);
    List<ProjectResponse> toProjectResponseList(List<Project> projects);
    @Mapping(source = "name", target = "projectName")
    ProjectSummaryResponse toProjectSummaryResponse (Project project);
    @Mapping(source = "name", target = "projectName")
    List<ProjectSummaryResponse>toProjectSummaryResponseList(List<Project> projects);
}
