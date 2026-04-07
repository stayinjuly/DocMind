package com.zm.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String email;

    public static AuthResponse of(String token, String email) {
        return AuthResponse.builder()
                .token(token)
                .email(email)
                .build();
    }
}
