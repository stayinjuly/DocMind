package com.zm.docmind.exception;

import org.springframework.http.HttpStatus;

/**
 * 邮箱已被注册，HTTP 409。
 */
public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
