package com.example.aicodehelper.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class AiModelConfig {

    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String apiKey;

    // 向量数据持久化路径
    private static final String EMBEDDING_STORE_PATH = "embedding-store.json";

    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        Path storePath = Paths.get(EMBEDDING_STORE_PATH);
        File storeFile = storePath.toFile();

        // 如果持久化文件存在，尝试加载
        if (storeFile.exists()) {
            try {
                InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromFile(storePath);
                log.info("✓ 从缓存加载向量数据: {} (大小: {} KB)",
                        EMBEDDING_STORE_PATH, storeFile.length() / 1024);
                log.info("✓ 跳过向量化处理，节省API调用");
                return store;
            } catch (Exception e) {
                log.warn("加载缓存失败，将重新创建: {}", e.getMessage());
            }
        } else {
            log.info("未找到向量数据缓存，将进行首次向量化（会消耗API额度）");
        }

        // 创建新的内存存储
        return new InMemoryEmbeddingStore<>();
    }
}
