package com.zm.docmind.service;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QaAssistant 管理器，负责为每个用户创建隔离的 AI 服务实例
 */
@Component
public class QaAssistantManager {

    private final ChatModel chatModel;
    private final ContentRetriever contentRetriever;
    private final ChatMemoryStore chatMemoryStore;

    // 缓存每个用户的 QaAssistant 实例
    private final Map<String, QaAssistant> assistantCache = new ConcurrentHashMap<>();

    // 记忆窗口大小
    private static final int MAX_MESSAGES = 10;

    public QaAssistantManager(ChatModel chatModel, ContentRetriever contentRetriever) {
        this.chatModel = chatModel;
        this.contentRetriever = contentRetriever;
        this.chatMemoryStore = new InMemoryChatMemoryStore();
    }

    /**
     * 获取或创建指定用户的 QaAssistant
     */
    public QaAssistant getAssistant(String userId) {
        return assistantCache.computeIfAbsent(userId, this::createAssistant);
    }

    private QaAssistant createAssistant(String userId) {
        // 为每个用户创建独立的 ChatMemory
        var chatMemory = MessageWindowChatMemory.builder()
                .id(userId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();

        return AiServices.builder(QaAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 清除指定用户的对话历史
     */
    public void clearUserHistory(String userId) {
        chatMemoryStore.deleteMessages(userId);
        assistantCache.remove(userId);
    }
}
