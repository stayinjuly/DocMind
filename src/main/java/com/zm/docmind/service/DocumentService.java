package com.zm.docmind.service;

import com.zm.docmind.entity.Document;
import com.zm.docmind.entity.DocumentStatus;
import com.zm.docmind.exception.BusinessException;
import com.zm.docmind.exception.InvalidInputException;
import com.zm.docmind.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 文档管理服务
 * 负责文档的上传、存储和删除
 */
@Slf4j
@Service
public class DocumentService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md", "pdf", "docx", "doc", "xlsx", "xls", "csv");

    /**
     * 扩展名 -> 允许的 MIME 类型前缀映射，用于校验文件实际内容类型
     */
    private static final Map<String, Set<String>> EXTENSION_MIME_MAP = Map.of(
            "txt", Set.of("text/plain"),
            "md", Set.of("text/plain", "text/markdown"),
            "pdf", Set.of("application/pdf"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "doc", Set.of("application/msword"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            "xls", Set.of("application/vnd.ms-excel"),
            "csv", Set.of("text/plain", "text/csv")
    );

    @Value("${docmind.storage.max-file-size:10}")
    private int maxFileSize;

    @Value("${docmind.storage.path}")
    private String storagePath;

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentProcessingService documentProcessingService) {
        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
    }

    @PostConstruct
    public void init() {
        Path path = Paths.get(storagePath);
        try {
            Files.createDirectories(path);
            log.info("文档存储目录初始化完成: {}", storagePath);
        } catch (IOException e) {
            log.error("创建文档存储目录失败: {}", storagePath, e);
            throw new IllegalStateException("创建文档存储目录失败: " + storagePath, e);
        }
    }

    public Page<Document> getDocumentsByUser(String userId, Pageable pageable) {
        return documentRepository.findByUserId(userId, pageable);
    }

    public Page<Document> getPublicDocuments(Pageable pageable) {
        return documentRepository.findByIsPublicTrue(pageable);
    }

    public Optional<Document> getDocument(String id) {
        return documentRepository.findById(id);
    }

    /**
     * 获取文档并校验用户所有权，公开文档所有人可访问
     *
     * @return 文档对象，如果无权限则抛出 AccessDeniedException
     * @throws AccessDeniedException 文档不属于该用户且非公开
     * @throws NoSuchElementException 文档不存在
     */
    public Document getDocumentForUser(String id, String userId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("文档不存在"));
        if (!document.isPublic() && !userId.equals(document.getUserId())) {
            throw new AccessDeniedException("无权访问他人的文档");
        }
        return document;
    }

    /**
     * 获取文档并校验用户所有权（仅限所有者）
     */
    public Document getOwnedDocument(String id, String userId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("文档不存在"));
        if (!userId.equals(document.getUserId())) {
            throw new AccessDeniedException("无权操作他人的文档");
        }
        return document;
    }

    public String uploadDocument(MultipartFile file, String userId, boolean isPublic) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new InvalidInputException("文件名无效");
        }

        if (file.getSize() > (long) maxFileSize * 1024 * 1024) {
            throw new InvalidInputException("文件过大，最大支持 " + maxFileSize + "MB");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_TYPES.contains(extension)) {
            throw new InvalidInputException("不支持的文件类型，仅支持 TXT、Markdown、PDF、Word、Excel 和 CSV 文件");
        }

        // 校验文件实际内容类型，防止伪装扩展名上传恶意文件
        String contentType = file.getContentType();
        Set<String> allowedMimes = EXTENSION_MIME_MAP.get(extension);
        if (contentType != null && allowedMimes != null) {
            boolean mimeMatched = allowedMimes.stream().anyMatch(contentType::startsWith);
            if (!mimeMatched) {
                log.warn("文件内容类型不匹配: 扩展名={}, Content-Type={}, 文件名={}", extension, contentType, originalFilename);
                throw new InvalidInputException("文件内容类型与扩展名不匹配，请检查文件是否合法");
            }
        }

        String documentId = UUID.randomUUID().toString();
        Path filePath;
        try {
            filePath = saveFile(file, documentId, extension);
        } catch (IOException e) {
            log.error("文档保存失败: {}", originalFilename, e);
            throw new BusinessException("文档上传失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Document document = Document.builder()
                .id(documentId)
                .name(originalFilename)
                .type(extension)
                .size(file.getSize())
                .filePath(filePath.toString())
                .isPublic(isPublic)
                .uploadTime(LocalDateTime.now())
                .userId(userId)
                .status(DocumentStatus.PENDING)
                .build();

        documentRepository.save(document);

        // 异步执行文档解析和向量化，立即返回响应
        documentProcessingService.processDocumentAsync(documentId, filePath, originalFilename, userId, isPublic);

        log.info("文档上传已接受: {} ({}), 用户: {}, 公开: {}", originalFilename, documentId, userId, isPublic);
        return documentId;
    }

    @Transactional
    public void deleteDocument(String id, String userId) {
        // 复用所有权校验：文档不存在抛 NoSuchElementException，非所有者抛 AccessDeniedException（纵深防御）
        Document document = getOwnedDocument(id, userId);

        documentRepository.deleteById(id);
        documentProcessingService.deleteEmbeddings(id);
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.error("删除文件失败: {}", document.getFilePath(), e);
        }

        log.info("文档删除成功: {}", id);
    }

    private Path saveFile(MultipartFile file, String documentId, String extension) throws IOException {
        String filename = documentId + "." + extension;
        Path filePath = Paths.get(storagePath, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}
