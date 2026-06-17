package com.zm.docmind.exception;

import org.springframework.http.HttpStatus;

/**
 * 触发限流（请求过于频繁），HTTP 429。
 */
public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
