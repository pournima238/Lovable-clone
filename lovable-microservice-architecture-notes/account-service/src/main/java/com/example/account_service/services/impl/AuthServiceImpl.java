package com.example.account_service.services.impl;


import com.example.account_service.dto.auth.AuthResponse;
import com.example.account_service.dto.auth.LoginRequest;
import com.example.account_service.dto.auth.SignUpRequest;
import com.example.account_service.entity.User;
import com.example.account_service.mapper.UserMapper;
import com.example.account_service.repository.UserRepository;
import com.example.account_service.services.AuthService;
import com.example.common_lib.error.BadRequestException;
import com.example.common_lib.security.AuthUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    AuthUtil authUtil;
    AuthenticationManager authenticationManager;

    @Override
    public AuthResponse signUp(SignUpRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BadRequestException("User already exists with username");
        });
        User user = userMapper.fromRequestToUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        String token = authUtil.generateAccessToken(userMapper.toUserDto(user));
        return new AuthResponse(token, userMapper.toUserProfileResponse(user));

    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(), request.password()
        ));
        User user = (User) authentication.getPrincipal();
        String token = authUtil.generateAccessToken(userMapper.toUserDto(user));
        return new AuthResponse(token, userMapper.toUserProfileResponse(user));

    }
}
