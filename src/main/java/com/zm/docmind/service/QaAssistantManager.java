package com.zm.docmind.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
    private final Cache<String, QaAssistant> assistantCache;

    private static final int MAX_MESSAGES = 10;

    private final int maxResults;
    private final double minScore;

    public QaAssistantManager(ChatModel chatModel,
                              StreamingChatModel streamingChatModel,
                              EmbeddingStore<TextSegment> embeddingStore,
                              EmbeddingModel embeddingModel,
                              @Value("${docmind.rag.max-results:5}") int maxResults,
                              @Value("${docmind.rag.min-score:0.6}") double minScore) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatMemoryStore = new InMemoryChatMemoryStore();
        this.maxResults = maxResults;
        this.minScore = minScore;
        this.assistantCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(Duration.ofMinutes(30))
                .removalListener((String key, QaAssistant value, RemovalCause cause) -> {
                    if (key != null && cause.wasEvicted()) {
                        chatMemoryStore.deleteMessages(key);
                        log.info("Caffeine 淘汰不活跃用户 assistant: userId={}, cause={}", key, cause);
                    }
                })
                .build();
    }

    public QaAssistant getAssistant(String userId) {
        return assistantCache.get(userId, this::createAssistant);
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
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    public void clearUserHistory(String userId) {
        assistantCache.invalidate(userId);
        chatMemoryStore.deleteMessages(userId);
    }
}
