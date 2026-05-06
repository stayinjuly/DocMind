package com.zm.docmind.controller;

import com.zm.docmind.dto.ApiResponse;
import com.zm.docmind.dto.AuthRequest;
import com.zm.docmind.dto.AuthResponse;
import com.zm.docmind.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok("注册成功", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok("登录成功", response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        String message = e.getMessage();
        HttpStatus status = message.contains("已被注册") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        if (message.contains("密码错误") || message.contains("不存在")) {
            status = HttpStatus.UNAUTHORIZED;
        }
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message));
    }
}
