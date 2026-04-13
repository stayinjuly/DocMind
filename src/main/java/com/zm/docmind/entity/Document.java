package com.zm.docmind.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 文档实体，表示上传的文档元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("doc_document")
public class Document {

    @Id
    private String id;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文件类型（txt, md, pdf, docx）
     */
    private String type;

    /**
     * 文件大小（字节）
     */
    private long size;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 是否公共文档
     */
    private boolean isPublic;

    /**
     * 分块数量
     */
    private int chunkCount;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传用户ID（邮箱）
     */
    private String userId;
}
