package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.auth.SignUpRequest;
import com.example.aiproject.lovable_clone.dto.auth.UserProfileResponse;
import com.example.aiproject.lovable_clone.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface UserMapper {
    User fromRequestToUser(SignUpRequest request);
    UserProfileResponse toUserProfileResponse(User user);
}
