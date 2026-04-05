package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.auth.UserProfileResponse;

public interface UserService {
    UserProfileResponse getProfile(Long userId);
}
