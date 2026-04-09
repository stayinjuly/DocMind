package com.zm.docmind.service;

import com.zm.docmind.entity.Document;
import com.zm.docmind.dto.DocumentUploadResponse;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档管理服务
 * 负责文档的上传、存储、解析和向量化
 */
@Slf4j
@Service
public class DocumentService {

    @Value("${docmind.storage.path}")
    private String storagePath;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();

    // 记录每个文档对应的嵌入向量 ID，用于删除时清理
    private final Map<String, List<String>> documentEmbeddingIds = new ConcurrentHashMap<>();

    public DocumentService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
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
        return new ArrayList<>(documentStore.values());
    }

    /**
     * 获取指定用户的文档列表
     */
    public List<Document> getDocumentsByUser(String userId) {
        return documentStore.values().stream()
                .filter(doc -> userId.equals(doc.getUserId()))
                .toList();
    }

    public Document getDocument(String id) {
        return documentStore.get(id);
    }

    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10_000_000;

    public DocumentUploadResponse uploadDocument(MultipartFile file, String userId) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return DocumentUploadResponse.error("文件名无效");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return DocumentUploadResponse.error("文件过大，最大支持 10MB");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isSupportedFileType(extension)) {
            return DocumentUploadResponse.error("不支持的文件类型，仅支持 TXT 和 Markdown 文件");
        }

        Path filePath = null;
        String documentId = null;

        try {
            documentId = UUID.randomUUID().toString();
            filePath = saveFile(file, documentId, extension);
            String content = Files.readString(filePath, StandardCharsets.UTF_8);

            Document document = Document.builder()
                    .id(documentId)
                    .name(originalFilename)
                    .type(extension)
                    .size(file.getSize())
                    .filePath(filePath.toString())
                    .uploadTime(LocalDateTime.now())
                    .userId(userId)
                    .build();

            documentStore.put(documentId, document);
            embedDocument(documentId, content);

            log.info("文档上传成功: {} ({}), 用户: {}", originalFilename, documentId, userId);
            return DocumentUploadResponse.success(documentId);

        } catch (IOException e) {
            log.error("文件保存失败: {}", originalFilename, e);
            // Cleanup on failure
            if (documentId != null) {
                documentStore.remove(documentId);
            }
            if (filePath != null) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ignored) {}
            }
            return DocumentUploadResponse.error("文件保存失败");
        } catch (Exception e) {
            log.error("文档处理失败: {}", originalFilename, e);
            // Cleanup on failure
            if (documentId != null) {
                documentStore.remove(documentId);
            }
            if (filePath != null) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ignored) {}
            }
            return DocumentUploadResponse.error("文档处理失败");
        }
    }

    public boolean deleteDocument(String id) {
        Document document = documentStore.remove(id);
        if (document == null) {
            return false;
        }

        // 清理关联的向量嵌入
        List<String> embeddingIds = documentEmbeddingIds.remove(id);
        if (embeddingIds != null) {
            embeddingStore.removeAll(embeddingIds);
            log.info("已清理文档 {} 的 {} 个嵌入向量", id, embeddingIds.size());
        }

        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
            log.info("文档删除成功: {}", id);
            return true;
        } catch (IOException e) {
            log.error("删除文件失败: {}", document.getFilePath(), e);
            return false;
        }
    }

    private Path saveFile(MultipartFile file, String documentId, String extension) throws IOException {
        String filename = documentId + "." + extension;
        Path filePath = Paths.get(storagePath, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    private void embedDocument(String documentId, String content) {
        List<String> chunks = splitIntoChunks(content, 500);
        List<String> embeddingIds = new ArrayList<>();

        for (String chunk : chunks) {
            if (chunk.trim().isEmpty()) {
                continue;
            }

            TextSegment segment = TextSegment.from(chunk);

            Embedding embedding = embeddingModel.embed(segment).content();
            String embeddingId = embeddingStore.add(embedding, segment);
            embeddingIds.add(embeddingId);
        }

        documentEmbeddingIds.put(documentId, embeddingIds);
        log.info("文档向量化完成: {}, 共 {} 个分块", documentId, chunks.size());
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

    private boolean isSupportedFileType(String extension) {
        return "txt".equals(extension) || "md".equals(extension);
    }
}
