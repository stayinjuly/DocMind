package com.zm.docmind.service;

import com.zm.docmind.dto.AuthResponse;
import com.zm.docmind.entity.User;
import com.zm.docmind.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务
 * 负责用户注册和登录逻辑
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 用户注册
     */
    public AuthResponse register(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码不能少于6位");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("该邮箱已被注册");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        String token = jwtService.generateToken(email);
        return AuthResponse.of(token, email);
    }

    /**
     * 用户登录
     */
    public AuthResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("邮箱或密码错误"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }

        String token = jwtService.generateToken(email);
        return AuthResponse.of(token, email);
    }
}
