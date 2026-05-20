package com.zm.docmind.service;

import com.zm.docmind.entity.Document;
import com.zm.docmind.entity.DocumentStatus;
import com.zm.docmind.repository.DocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 文档异步处理服务
 * 独立 Bean 以确保 Spring AOP 代理正确处理 @Async 注解
 */
@Slf4j
@Service
public class DocumentProcessingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentRepository documentRepository;
    private final DocumentParserService documentParserService;

    @Value("${docmind.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${docmind.rag.chunk-overlap:100}")
    private int chunkOverlap;

    public DocumentProcessingService(EmbeddingModel embeddingModel,
                                     EmbeddingStore<TextSegment> embeddingStore,
                                     DocumentRepository documentRepository,
                                     DocumentParserService documentParserService) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentRepository = documentRepository;
        this.documentParserService = documentParserService;
    }

    @Async
    public void processDocumentAsync(String documentId, Path filePath, String documentName, String userId, boolean isPublic) {
        try {
            updateDocumentStatus(documentId, DocumentStatus.PROCESSING);

            String content = documentParserService.parseDocument(filePath);
            int chunkCount = embedDocument(documentId, content, documentName, userId, isPublic);

            Document doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null) {
                doc.setChunkCount(chunkCount);
                doc.setStatus(DocumentStatus.COMPLETED);
                doc.setNewEntity(false);
                documentRepository.save(doc);
            }

            log.info("文档处理完成: {} ({}), 分块数: {}", documentName, documentId, chunkCount);

        } catch (Exception e) {
            log.error("文档异步处理失败: {} ({})", documentName, documentId, e);
            updateDocumentStatus(documentId, DocumentStatus.FAILED);
            cleanupOnFailure(documentId, filePath);
        }
    }

    private void updateDocumentStatus(String documentId, DocumentStatus status) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc != null) {
            doc.setStatus(status);
            doc.setNewEntity(false);
            documentRepository.save(doc);
        }
    }

    private int embedDocument(String documentId, String content, String documentName, String userId, boolean isPublic) {
        dev.langchain4j.data.document.DocumentSplitter splitter =
                dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(chunkSize, chunkOverlap);

        dev.langchain4j.data.document.Document langchainDoc =
                dev.langchain4j.data.document.Document.from(content);
        List<TextSegment> rawSegments = splitter.split(langchainDoc);

        List<TextSegment> segments = new ArrayList<>();
        for (TextSegment raw : rawSegments) {
            if (raw.text().trim().isEmpty()) {
                continue;
            }
            dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
            metadata.put("userId", userId);
            metadata.put("documentId", documentId);
            metadata.put("documentName", documentName);
            metadata.put("isPublic", String.valueOf(isPublic));

            segments.add(new TextSegment(raw.text(), metadata));
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        log.info("文档向量化完成: {}, 共 {} 个分块 (chunkSize={}, overlap={})", documentId, segments.size(), chunkSize, chunkOverlap);
        return segments.size();
    }

    private void cleanupOnFailure(String documentId, Path filePath) {
        if (documentId != null) {
            try {
                embeddingStore.removeAll(metadataKey("documentId").isEqualTo(documentId));
            } catch (Exception e) {
                log.warn("清理嵌入向量失败: documentId={}", documentId, e);
            }
        }
        if (filePath != null) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("删除文件失败: {}", filePath, e);
            }
        }
    }

    public void deleteEmbeddings(String documentId) {
        try {
            embeddingStore.removeAll(metadataKey("documentId").isEqualTo(documentId));
            log.info("已清理文档 {} 的嵌入向量", documentId);
        } catch (Exception e) {
            log.warn("清理嵌入向量失败: {}", documentId, e);
        }
    }
}
