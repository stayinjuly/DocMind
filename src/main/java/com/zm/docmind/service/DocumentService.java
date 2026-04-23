package com.zm.docmind.service;

import com.zm.docmind.entity.Document;
import com.zm.docmind.dto.DocumentUploadResponse;
import com.zm.docmind.repository.DocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 文档管理服务
 * 负责文档的上传、存储、解析和向量化
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

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentRepository documentRepository;
    private final DocumentParserService documentParserService;

    public DocumentService(EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore,
                           DocumentRepository documentRepository,
                           DocumentParserService documentParserService) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentRepository = documentRepository;
        this.documentParserService = documentParserService;
    }

    @PostConstruct
    public void init() {
        Path path = Paths.get(storagePath);
        try {
            Files.createDirectories(path);
            log.info("文档存储目录初始化完成: {}", storagePath);
        } catch (IOException e) {
            log.error("创建文档存储目录失败: {}", storagePath, e);
        }
    }

    public List<Document> getAllDocuments() {
        List<Document> docs = new ArrayList<>();
        documentRepository.findAll().forEach(docs::add);
        return docs;
    }

    public List<Document> getDocumentsByUser(String userId) {
        return documentRepository.findByUserId(userId);
    }

    public List<Document> getPublicDocuments() {
        return documentRepository.findByIsPublicTrue();
    }

    public Document getDocument(String id) {
        return documentRepository.findById(id).orElse(null);
    }

    public DocumentUploadResponse uploadDocument(MultipartFile file, String userId, boolean isPublic) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return DocumentUploadResponse.error("文件名无效");
        }

        if (file.getSize() > (long) maxFileSize * 1_000_000) {
            return DocumentUploadResponse.error("文件过大，最大支持 " + maxFileSize + "MB");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_TYPES.contains(extension)) {
            return DocumentUploadResponse.error("不支持的文件类型，仅支持 TXT、Markdown、PDF 和 Word 文件");
        }

        // 校验文件实际内容类型，防止伪装扩展名上传恶意文件
        String contentType = file.getContentType();
        Set<String> allowedMimes = EXTENSION_MIME_MAP.get(extension);
        if (contentType != null && allowedMimes != null) {
            boolean mimeMatched = allowedMimes.stream().anyMatch(contentType::startsWith);
            if (!mimeMatched) {
                log.warn("文件内容类型不匹配: 扩展名={}, Content-Type={}, 文件名={}", extension, contentType, originalFilename);
                return DocumentUploadResponse.error("文件内容类型与扩展名不匹配，请检查文件是否合法");
            }
        }

        Path filePath = null;
        String documentId = null;

        try {
            documentId = UUID.randomUUID().toString();
            filePath = saveFile(file, documentId, extension);

            // 使用 Tika 解析文档提取文本
            String content = documentParserService.parseDocument(filePath);

            Document document = Document.builder()
                    .id(documentId)
                    .name(originalFilename)
                    .type(extension)
                    .size(file.getSize())
                    .filePath(filePath.toString())
                    .isPublic(isPublic)
                    .uploadTime(LocalDateTime.now())
                    .userId(userId)
                    .build();

            // 先保存文档元数据，确保数据入库
            documentRepository.save(document);

            int chunkCount = embedDocument(documentId, content, userId, isPublic);
            document.setChunkCount(chunkCount);
            document.setNewEntity(false);
            documentRepository.save(document);

            log.info("文档上传成功: {} ({}), 用户: {}, 公开: {}", originalFilename, documentId, userId, isPublic);
            return DocumentUploadResponse.success(documentId);

        } catch (Exception e) {
            log.error("文档处理失败: {}", originalFilename, e);
            cleanupOnFailure(documentId, filePath);
            if (e instanceof RuntimeException rt && rt.getMessage() != null && rt.getMessage().startsWith("文档解析失败")) {
                return DocumentUploadResponse.error("文档解析失败，请检查文件是否损坏");
            }
            return DocumentUploadResponse.error("文档处理失败: " + e.getMessage());
        }
    }

    public boolean deleteDocument(String id) {
        Document document = documentRepository.findById(id).orElse(null);
        if (document == null) {
            return false;
        }

        // 使用元数据过滤删除该文档的所有向量
        try {
            embeddingStore.removeAll(metadataKey("documentId").isEqualTo(id));
            log.info("已清理文档 {} 的嵌入向量", id);
        } catch (Exception e) {
            log.warn("清理嵌入向量失败: {}", id, e);
        }

        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.error("删除文件失败: {}", document.getFilePath(), e);
        }

        documentRepository.deleteById(id);
        log.info("文档删除成功: {}", id);
        return true;
    }

    private Path saveFile(MultipartFile file, String documentId, String extension) throws IOException {
        String filename = documentId + "." + extension;
        Path filePath = Paths.get(storagePath, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    private int embedDocument(String documentId, String content, String userId, boolean isPublic) {
        List<String> rawChunks = splitIntoChunks(content, 500);
        List<TextSegment> segments = new ArrayList<>();

        for (String chunk : rawChunks) {
            if (chunk.trim().isEmpty()) {
                continue;
            }

            dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
            metadata.put("userId", userId);
            metadata.put("documentId", documentId);
            metadata.put("isPublic", String.valueOf(isPublic));

            segments.add(new TextSegment(chunk, metadata));
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        log.info("文档向量化完成: {}, 共 {} 个分块", documentId, segments.size());
        return segments.size();
    }

    private List<String> splitIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > maxChunkSize && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private void cleanupOnFailure(String documentId, Path filePath) {
        if (documentId != null) {
            try {
                embeddingStore.removeAll(metadataKey("documentId").isEqualTo(documentId));
            } catch (Exception ignored) {}
            try {
                documentRepository.deleteById(documentId);
            } catch (Exception ignored) {}
        }
        if (filePath != null) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {}
        }
    }
}
