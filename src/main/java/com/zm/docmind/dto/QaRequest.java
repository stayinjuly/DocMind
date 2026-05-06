package com.zm.docmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Q&A 请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000字符")
    private String question;
}
