package com.zm.docmind.dto;

import com.zm.docmind.entity.Document;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档 API 响应对象，隐藏 filePath 等内部字段
 */
@Data
@Builder
public class DocumentVO {
    private String id;
    private String name;
    private String type;
    private long size;
    private boolean isPublic;
    private int chunkCount;
    private String status;
    private LocalDateTime uploadTime;
    private String userId;

    public static DocumentVO from(Document doc) {
        return DocumentVO.builder()
                .id(doc.getId())
                .name(doc.getName())
                .type(doc.getType())
                .size(doc.getSize())
                .isPublic(doc.isPublic())
                .chunkCount(doc.getChunkCount())
                .status(doc.getStatus())
                .uploadTime(doc.getUploadTime())
                .userId(doc.getUserId())
                .build();
    }
}
