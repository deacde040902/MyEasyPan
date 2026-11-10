package com.easypan.rag.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.io.File;

import jakarta.annotation.Resource;

/**
 * Spring AI 模型装配（阿里云百炼 OpenAI 兼容模式）
 * <p>
 * 对话模型：qwen-plus；向量化模型：text-embedding-v3（1024 维）。
 * 向量库使用 Spring AI 内置 SimpleVectorStore（JSON 落盘持久化），
 * 后续如切换 Redis Stack / Milvus，只需替换本类中的 VectorStore 装配。
 */
@Configuration
public class AiConfig {

    @Resource
    private RagProperties ragProperties;

    @Value("${easypan.file.base-path}")
    private String fileBasePath;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(ragProperties.getBaseUrl())
                .apiKey(ragProperties.getApiKey())
                .build();
    }

    @Bean
    public OpenAiChatModel chatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(ragProperties.getChatModel())
                        .temperature(0.3)
                        .build())
                .build();
    }

    @Bean
    public OpenAiEmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(ragProperties.getEmbeddingModel())
                        .dimensions(ragProperties.getEmbeddingDimensions())
                        .build(),
                RetryTemplate.builder().maxAttempts(3).build());
    }

    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File persistFile = vectorStoreFile();
        if (persistFile.exists()) {
            // 启动时加载已有向量索引，避免重启后全量重建
            store.load(persistFile);
        }
        return store;
    }

    public static File vectorStoreFile(String fileBasePath) {
        File dir = new File(fileBasePath, "rag");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "vectorstore.json");
    }

    private File vectorStoreFile() {
        return vectorStoreFile(fileBasePath);
    }
}
