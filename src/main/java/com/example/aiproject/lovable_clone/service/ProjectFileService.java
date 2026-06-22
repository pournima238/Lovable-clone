package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.project.FileContentResponse;
import com.example.aiproject.lovable_clone.dto.project.FileNode;
import com.example.aiproject.lovable_clone.dto.project.FileTreeResponse;

import java.util.List;

public interface ProjectFileService {
//    List<FileNode> getFileTree(Long projectId, Long userId);

//    FileContentResponse getFileContent(Long projectId, String path, Long userId);

    FileTreeResponse getFileTree(Long projectId, Long userId);

    FileContentResponse getFileContent(Long projectId, String path, Long userId);

    void saveFile(Long projectId, String filePath, String fileContent);
}
