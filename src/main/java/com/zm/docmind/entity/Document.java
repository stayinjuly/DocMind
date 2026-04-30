package com.zm.docmind.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
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
public class Document implements Persistable<String> {

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
     * 处理状态：PENDING(待处理)、PROCESSING(处理中)、COMPLETED(完成)、FAILED(失败)
     */
    private String status;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传用户ID（邮箱）
     */
    private String userId;

    /**
     * 标记实体是否为新创建（未被持久化）
     * Spring Data JDBC 通过 @Id 是否为 null 判断 INSERT/UPDATE，
     * 但我们的 ID 是预先生成的 UUID（永远不为 null），会导致 save() 误执行 UPDATE。
     * 通过 Persistable 接口显式控制此行为。
     */
    @Transient
    @Builder.Default
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }
}
