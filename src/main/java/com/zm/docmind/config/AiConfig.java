package com.zm.docmind.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 核心 Bean 配置：
 * - EmbeddingModel: 用于生成向量
 * - EmbeddingStore: 向量存储（PostgreSQL pgvector）
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

    @Value("${dashscope.embedding-model}")
    private String embeddingModelName;

    @Value("${dashscope.api_key}")
    private String dashscopeApiKey;

    @Value("${docmind.embedding.table}")
    private String embeddingTable;

    @Value("${docmind.embedding.dimension}")
    private int embeddingDimension;

    /** Embedding 模型，用于生成文本向量 */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(embeddingModelName)
                .build();
    }

    /** PgVector 向量存储 */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
        log.info("初始化 PgVector 向量存储，表: {}, 维度: {}", embeddingTable, embeddingDimension);
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(embeddingTable)
                .dimension(embeddingDimension)
                .createTable(true)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(java.util.Collections.singletonList("metadata JSONB NULL"))
                        .build())
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
}
