package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.auth.AuthResponse;
import com.example.aiproject.lovable_clone.dto.auth.LoginRequest;
import com.example.aiproject.lovable_clone.dto.auth.SignUpRequest;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.error.BadRequestException;
import com.example.aiproject.lovable_clone.mapper.UserMapper;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    @Override
    public AuthResponse signUp(SignUpRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user->{
            throw new BadRequestException("User already exists with username");
        });
        User user = userMapper.fromRequestToUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return new AuthResponse("dummyToken",userMapper.toUserProfileResponse(user));

    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return null;
    }
}
