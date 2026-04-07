package com.zm.docmind.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 核心 Bean 配置：
 * - EmbeddingModel: 用于生成向量
 * - EmbeddingStore: 向量存储（内存实现）
 * - ContentRetriever: 基于向量的检索器
 * - ChatModel: 用于对话的语言模型
 */
@Slf4j
@Configuration
public class AiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String modelName;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.embedding-model}")
    private String embeddingModelName;

    @Value("${dashscope.api_key}")
    private String dashscopeApiKey;

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    /** Embedding 模型，用于生成文本向量 */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(dashscopeApiKey) // 替换为你的 Key
                .modelName(embeddingModelName)
                .build();
    }

       /** 内存向量存储 */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return embeddingStore;
    }

    /** 内容检索器（基于向量相似度） */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.6)
                .build();
    }

    /** Chat 模型 */
    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

//    /** 初始化知识库数据 */
//    @Bean
//    public ApplicationRunner initKnowledgeBase(EmbeddingModel embeddingModel) {
//        return args -> {
//            log.info("正在初始化知识库...");
//
//            try {
//                List<String> documents = List.of(
//                    "Java 25 引入了虚拟线程的进一步优化。",
//                    "Spring Boot 3.2 版本原生支持了观察性指标。",
//                    "LangChain4j 是一个纯 Java 实现的框架。",
//                    "本系统支持多用户隔离对话。",
//                    "OpenAI 的 text-embedding-3-small 模型性价比很高。"
//                );
//
//                for (String text : documents) {
//                    TextSegment segment = TextSegment.from(text);
//                    Embedding embedding = embeddingModel.embed(segment).content();
//                    embeddingStore.add(embedding, segment);
//                }
//
//                log.info("知识库初始化完成，共 {} 条文档", documents.size());
//            } catch (Exception e) {
//                log.warn("知识库初始化失败，RAG 功能可能不可用: {}", e.getMessage());
//            }
//        };
//    }


}
