package com.zm.docmind.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * QaAssistant 管理器，负责为每个用户创建隔离的 AI 服务实例
 * 每个用户的检索范围：自己的文档 + 所有公共文档
 */
@Slf4j
@Component
public class QaAssistantManager {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatMemoryStore chatMemoryStore;

    private final Map<String, QaAssistant> assistantCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    private static final int MAX_MESSAGES = 10;
    private static final long EVICTION_THRESHOLD_MS = 30 * 60 * 1000;

    public QaAssistantManager(ChatModel chatModel,
                              StreamingChatModel streamingChatModel,
                              EmbeddingStore<TextSegment> embeddingStore,
                              EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatMemoryStore = new InMemoryChatMemoryStore();
    }

    public QaAssistant getAssistant(String userId) {
        lastAccessTime.put(userId, System.currentTimeMillis());
        return assistantCache.computeIfAbsent(userId, this::createAssistant);
    }

    private QaAssistant createAssistant(String userId) {
        var chatMemory = MessageWindowChatMemory.builder()
                .id(userId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();

        Filter filter = metadataKey("userId").isEqualTo(userId)
                .or(metadataKey("isPublic").isEqualTo("true"));

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(filter)
                .maxResults(5)
                .minScore(0.6)
                .build();

        return AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    public void clearUserHistory(String userId) {
        chatMemoryStore.deleteMessages(userId);
        assistantCache.remove(userId);
        lastAccessTime.remove(userId);
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void evictInactiveAssistants() {
        long now = System.currentTimeMillis();
        lastAccessTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > EVICTION_THRESHOLD_MS) {
                assistantCache.remove(entry.getKey());
                chatMemoryStore.deleteMessages(entry.getKey());
                log.info("淘汰不活跃用户 assistant: userId={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
