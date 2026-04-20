package com.zm.docmind.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 核心 Bean 配置：
 * - ChatModel: 通过 OpenAI 兼容协议对接各厂商
 * - EmbeddingModel: 工厂模式，根据 docmind.embedding.provider 选择实现
 * - EmbeddingStore: 向量存储（PostgreSQL pgvector）
 */
@Slf4j
@Configuration
public class AiConfig {

    // Chat 模型配置
    @Value("${docmind.chat.base-url}")
    private String chatBaseUrl;

    @Value("${docmind.chat.api-key}")
    private String chatApiKey;

    @Value("${docmind.chat.model}")
    private String chatModelName;

    // Embedding 模型配置
    @Value("${docmind.embedding.provider}")
    private String embeddingProvider;

    @Value("${docmind.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${docmind.embedding.model}")
    private String embeddingModelName;

    @Value("${docmind.embedding.base-url:}")
    private String embeddingBaseUrl;

    @Value("${docmind.embedding.dimensions:}")
    private Integer embeddingDimensions;

    // 向量存储配置
    @Value("${docmind.embedding.table}")
    private String embeddingTable;

    @Value("${docmind.embedding.dimension}")
    private int embeddingDimension;

    /** Chat 模型，通过 OpenAI 兼容协议对接 */
    @Bean
    public ChatModel chatModel() {
        log.info("初始化 Chat 模型: baseUrl={}, model={}", chatBaseUrl, chatModelName);
        return OpenAiChatModel.builder()
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .baseUrl(chatBaseUrl)
                .build();
    }

    /** 流式 Chat 模型 */
    @Bean
    public StreamingChatModel streamingChatModel() {
        log.info("初始化 StreamingChat 模型: baseUrl={}, model={}", chatBaseUrl, chatModelName);
        return OpenAiStreamingChatModel.builder()
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .baseUrl(chatBaseUrl)
                .build();
    }

    /** Embedding 模型，根据 provider 配置自动选择实现 */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化 Embedding 模型: provider={}, model={}", embeddingProvider, embeddingModelName);
        return switch (embeddingProvider.toLowerCase()) {
            case "openai-compatible" -> OpenAiEmbeddingModel.builder()
                    .apiKey(embeddingApiKey)
                    .modelName(embeddingModelName)
                    .baseUrl(embeddingBaseUrl)
                    .build();
            case "dashscope" -> QwenEmbeddingModel.builder()
                    .apiKey(embeddingApiKey)
                    .modelName(embeddingModelName)
                    .build();
            case "zhipu" -> ZhipuAiEmbeddingModel.builder()
                    .apiKey(embeddingApiKey)
                    .model(embeddingModelName)
                    .dimensions(embeddingDimensions)
                    .build();
            default -> throw new IllegalArgumentException(
                    "未知的 Embedding provider: " + embeddingProvider
                    + "，支持的值: openai-compatible, dashscope, zhipu");
        };
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

    /** 启动时校验 Embedding 模型维度与 PgVector 表维度是否一致 */
    @Bean
    public ApplicationRunner embeddingDimensionValidator(EmbeddingModel model) {
        return args -> {
            int modelDim = model.dimension();
            if (modelDim != embeddingDimension) {
                String msg = String.format(
                        "Embedding 模型维度(%d)与 PgVector 表维度(%d)不一致！"
                        + "请修改 docmind.embedding.dimension 为 %d，"
                        + "并执行 DROP TABLE %s 后重启（需重新上传文档）",
                        modelDim, embeddingDimension, modelDim, embeddingTable);
                log.error(msg);
                throw new IllegalStateException(msg);
            }
            log.info("Embedding 维度校验通过: provider={}, model={}, dimension={}",
                    embeddingProvider, embeddingModelName, modelDim);
        };
    }
}
