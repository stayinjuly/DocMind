package com.zm.docmind.exception;

import org.springframework.http.HttpStatus;

/**
 * 输入校验失败（邮箱格式、密码强度、上传文件类型/大小等），HTTP 400。
 */
public class InvalidInputException extends BusinessException {

    public InvalidInputException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
