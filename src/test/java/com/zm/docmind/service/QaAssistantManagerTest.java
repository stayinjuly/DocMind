package com.zm.docmind.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class QaAssistantManagerTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private StreamingChatModel streamingChatModel;
    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;
    @Mock
    private EmbeddingModel embeddingModel;

    private QaAssistantManager manager;

    @BeforeEach
    void setUp() {
        manager = new QaAssistantManager(chatModel, streamingChatModel, embeddingStore, embeddingModel, 5, 0.6);
    }

    @Test
    @DisplayName("同一用户应返回缓存的同一实例")
    void sameUser_returnsCachedInstance() {
        var assistant1 = manager.getAssistant("user1");
        var assistant2 = manager.getAssistant("user1");

        assertThat(assistant1).isSameAs(assistant2);
    }

    @Test
    @DisplayName("不同用户应返回不同实例")
    void differentUsers_returnsDifferentInstances() {
        var assistant1 = manager.getAssistant("user1");
        var assistant2 = manager.getAssistant("user2");

        assertThat(assistant1).isNotSameAs(assistant2);
    }

    @Test
    @DisplayName("清除历史后再次获取应返回新实例")
    void clearHistory_returnsNewInstance() {
        var assistant1 = manager.getAssistant("user1");

        manager.clearUserHistory("user1");

        var assistant2 = manager.getAssistant("user1");
        assertThat(assistant2).isNotSameAs(assistant1);
    }
}
