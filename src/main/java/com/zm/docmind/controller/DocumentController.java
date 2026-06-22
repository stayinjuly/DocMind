package com.zm.docmind.controller;

import com.zm.docmind.dto.ApiResponse;
import com.zm.docmind.dto.DocumentVO;
import com.zm.docmind.entity.Document;
import com.zm.docmind.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理控制器
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<Page<DocumentVO>> listDocuments(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DocumentVO> result = documentService.getDocumentsByUser(userId, PageRequest.of(page, size))
                .map(DocumentVO::from);
        return ApiResponse.ok(result);
    }

    @GetMapping("/public")
    public ApiResponse<Page<DocumentVO>> listPublicDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DocumentVO> result = documentService.getPublicDocuments(PageRequest.of(page, size))
                .map(DocumentVO::from);
        return ApiResponse.ok(result);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal String userId) {
        String documentId = documentService.uploadDocument(file, userId, isPublic);
        return ResponseEntity.ok(ApiResponse.ok("文档上传成功", documentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable String id,
                                                             @AuthenticationPrincipal String userId) {
        documentService.deleteDocument(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("文档删除成功", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentVO>> getDocument(@PathVariable String id,
                                                                 @AuthenticationPrincipal String userId) {
        Document document = documentService.getDocumentForUser(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(DocumentVO.from(document)));
    }
}
