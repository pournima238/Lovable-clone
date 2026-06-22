package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.project.FileNode;
import com.example.aiproject.lovable_clone.entity.ProjectFile;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectFileMapper {

    List<FileNode> toListOfFileNode(List<ProjectFile> projectFileList);
}

