package com.zm.docmind.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常基类，携带 HTTP 状态码，由 GlobalExceptionHandler 统一映射为 {@code ApiResponse} 响应。
 * 各具体业务异常通过子类固定状态码，避免在 Controller 中用字符串匹配判断 HTTP 状态。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
