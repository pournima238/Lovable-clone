package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.deploy.DeployResponse;

public interface DeploymentService {
    DeployResponse deploy(Long projectId);
}
