package com.zm.docmind.exception;

import org.springframework.http.HttpStatus;

/**
 * 登录凭据无效（邮箱不存在或密码错误），HTTP 401。
 * <p>对“邮箱不存在”和“密码错误”统一返回相同文案，避免通过错误信息探测系统中已注册的邮箱。
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
