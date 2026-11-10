package com.easypan.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * RAG 模块配置项（来源 classpath:rag.properties）
 */
@Data
@Component
@PropertySource("classpath:rag.properties")
@ConfigurationProperties(prefix = "easypan.rag")
public class RagProperties {

    /** 阿里云百炼 API Key */
    private String apiKey;

    /** OpenAI 兼容模式基地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode";

    /** 对话模型 */
    private String chatModel = "qwen-plus";

    /** 向量化模型（BGE 系列，中文语义表征） */
    private String embeddingModel = "text-embedding-v3";

    /** 向量维度 */
    private Integer embeddingDimensions = 1024;

    /** 分块目标长度（字符） */
    private Integer chunkSize = 600;

    /** 相邻分块重叠长度（字符） */
    private Integer chunkOverlap = 150;

    /** 向量检索 Top-K */
    private Integer topK = 5;

    /** 相似度阈值，低于该值触发置信拒答 */
    private Double scoreThreshold = 0.35;

    /** 注入 Prompt 的上下文最大字符数 */
    private Integer maxContextChars = 6000;

    /** 小于该字符数的切片直接丢弃 */
    private Integer minChunkChars = 20;
}
