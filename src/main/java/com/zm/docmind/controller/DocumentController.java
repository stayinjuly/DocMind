package com.zm.docmind.controller;

import com.zm.docmind.dto.DocumentUploadResponse;
import com.zm.docmind.entity.Document;
import com.zm.docmind.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理控制器
 * 提供文档上传、列表、删除等 API，所有操作均基于当前登录用户隔离
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 获取当前用户的文档列表
     */
    @GetMapping
    public List<Document> listDocuments(@AuthenticationPrincipal String email) {
        return documentService.getDocumentsByUser(email);
    }

    /**
     * 上传文档
     * @param file 文档文件（TXT/Markdown）
     * @param email 当前登录用户邮箱（从 JWT 令牌中提取）
     */
    @PostMapping
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String email) {
        DocumentUploadResponse response = documentService.uploadDocument(file, email);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 删除文档（仅允许删除自己的文档）
     * @param id 文档ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id,
                                                  @AuthenticationPrincipal String email) {
        Document document = documentService.getDocument(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        if (!email.equals(document.getUserId())) {
            return ResponseEntity.status(403).body("无权删除他人的文档");
        }
        documentService.deleteDocument(id);
        return ResponseEntity.ok("文档删除成功");
    }

    /**
     * 获取单个文档信息（仅允许查看自己的文档）
     * @param id 文档ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id,
                                                 @AuthenticationPrincipal String email) {
        Document document = documentService.getDocument(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        if (!email.equals(document.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(document);
    }
}
