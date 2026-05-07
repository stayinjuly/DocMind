package com.zm.docmind.controller;

import com.zm.docmind.dto.ApiResponse;
import com.zm.docmind.dto.DocumentUploadResponse;
import com.zm.docmind.dto.DocumentVO;
import com.zm.docmind.entity.Document;
import com.zm.docmind.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DocumentVO> result = documentService.getDocumentsByUser(email, PageRequest.of(page, size))
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
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal String email) {
        DocumentUploadResponse response = documentService.uploadDocument(file, email, isPublic);
        if (response.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.ok("文档上传成功", response));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(response.getMessage()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable String id,
                                                             @AuthenticationPrincipal String email) {
        var doc = documentService.getDocument(id);
        if (doc.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("文档不存在"));
        }
        Document document = doc.get();
        if (!email.equals(document.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("无权删除他人的文档"));
        }
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.ok("文档删除成功", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentVO>> getDocument(@PathVariable String id,
                                                                 @AuthenticationPrincipal String email) {
        var doc = documentService.getDocument(id);
        if (doc.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("文档不存在"));
        }
        Document document = doc.get();
        if (!document.isPublic() && !email.equals(document.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("无权查看他人的文档"));
        }
        return ResponseEntity.ok(ApiResponse.ok(DocumentVO.from(document)));
    }
}
