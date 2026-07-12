package com.example.account_service.mapper;

import com.example.account_service.dto.auth.SignUpRequest;
import com.example.account_service.dto.auth.UserProfileResponse;
import com.example.account_service.entity.User;
import com.example.common_lib.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User fromRequestToUser(SignUpRequest request);

    UserProfileResponse toUserProfileResponse(User user);

    UserDto toUserDto(User user);
}
