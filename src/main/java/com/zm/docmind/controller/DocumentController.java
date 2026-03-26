package com.zm.docmind.controller;

import com.zm.docmind.dto.DocumentUploadResponse;
import com.zm.docmind.entity.Document;
import com.zm.docmind.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理控制器
 * 提供文档上传、列表、删除等 API
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 获取所有文档列表
     */
    @GetMapping
    public List<Document> listDocuments() {
        return documentService.getAllDocuments();
    }

    /**
     * 上传文档
     * @param file 文档文件（TXT/Markdown）
     * @param userId 用户ID
     */
    @PostMapping
    public DocumentUploadResponse uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "default") String userId) {
        return documentService.uploadDocument(file, userId);
    }

    /**
     * 删除文档
     * @param id 文档ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id) {
        boolean deleted = documentService.deleteDocument(id);
        if (deleted) {
            return ResponseEntity.ok("文档删除成功");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取单个文档信息
     * @param id 文档ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {
        Document document = documentService.getDocument(id);
        if (document != null) {
            return ResponseEntity.ok(document);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
