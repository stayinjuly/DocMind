package com.zm.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证请求 DTO（注册/登录共用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    private String email;

    private String password;
}
