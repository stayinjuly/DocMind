package com.zm.docmind.service;

import com.zm.docmind.dto.AuthResponse;
import com.zm.docmind.entity.User;
import com.zm.docmind.exception.EmailAlreadyRegisteredException;
import com.zm.docmind.exception.InvalidCredentialsException;
import com.zm.docmind.exception.InvalidInputException;
import com.zm.docmind.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 认证服务
 * 负责用户注册和登录逻辑
 */
@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

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
            throw new InvalidInputException("邮箱不能为空");
        }
        email = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidInputException("邮箱格式不正确");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new InvalidInputException("密码不能少于6位");
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("该邮箱已被注册");
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
        email = email.trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("邮箱或密码错误"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("邮箱或密码错误");
        }

        String token = jwtService.generateToken(email);
        return AuthResponse.of(token, email);
    }
}
