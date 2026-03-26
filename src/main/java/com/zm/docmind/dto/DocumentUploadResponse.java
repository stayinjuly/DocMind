package com.zm.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private boolean success;
    private String message;
    private String documentId;

    public static DocumentUploadResponse success(String documentId) {
        return DocumentUploadResponse.builder()
                .success(true)
                .message("文档上传成功")
                .documentId(documentId)
                .build();
    }

    public static DocumentUploadResponse error(String message) {
        return DocumentUploadResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
