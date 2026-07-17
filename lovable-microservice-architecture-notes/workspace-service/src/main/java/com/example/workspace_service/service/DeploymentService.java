package com.example.workspace_service.service;


import com.example.workspace_service.dto.deploy.DeployResponse;

public interface DeploymentService {
    DeployResponse deploy(Long projectId);
}
