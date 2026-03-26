package com.zm.docmind.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档实体，表示上传的文档元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识
     */
    private String id;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文件类型（txt, md）
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
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传用户ID
     */
    private String userId;
}
