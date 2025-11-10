package com.example.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.List;

// 加载rag模型
@Configuration
@Slf4j
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Resource
    private EnhancedDocumentLoader enhancedDocumentLoader;

    @Value("${rag.documents-path}")
    private String documentsPath;

    @Bean
    public ContentRetriever contentRetriever() {
        // ------ 增强版RAG ------
        log.info("开始初始化RAG系统...");

        // 1. 使用增强的文档加载器加载所有支持格式的文档
        log.info("正在从目录加载文档: {}", documentsPath);
        log.info("支持的格式: {}", enhancedDocumentLoader.getSupportedFormats());

        List<Document> documents = enhancedDocumentLoader.loadAllDocuments(this.documentsPath);

        if (documents.isEmpty()) {
            log.warn("未找到任何文档，RAG功能将无法正常工作");
        } else {
            log.info("成功加载 {} 个文档，开始向量化处理...", documents.size());
        }

        // 2. 文档切割：将每个文档按每段进行分割，最大 1000 字符，每次重叠最多 200 个字符
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(1000, 200);

        // 3. 自定义文档加载器
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(paragraphSplitter)
                // 为了提高搜索质量，为每个 TextSegment 添加文档名称、类型和页码信息
                .textSegmentTransformer(textSegment -> {
                    // 获取原始metadata
                    Metadata originalMetadata = textSegment.metadata();

                    // 从metadata中获取信息（使用getString方法）
                    String fileName = originalMetadata.getString("file_name");
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = "未知文件";
                    }

                    String pageNumber = originalMetadata.getString("page_number");
                    String fileType = getFileTypeFromName(fileName);

                    // 构建增强的文本，包含文档信息
                    StringBuilder enhancedText = new StringBuilder();
                    enhancedText.append("[").append(fileName);
                    if (pageNumber != null && !pageNumber.isEmpty()) {
                        enhancedText.append(" - 第").append(pageNumber).append("页");
                    }
                    enhancedText.append(" - ").append(fileType).append("]\n");
                    enhancedText.append(textSegment.text());

                    // 创建新的Metadata对象：从原始metadata创建Map，然后更新
                    Map<String, String> metadataMap = new HashMap<>();

                    // 复制原有metadata中的值（如果存在）
                    if (originalMetadata.getString("file_name") != null) {
                        metadataMap.put("file_name", originalMetadata.getString("file_name"));
                    }
                    if (originalMetadata.getString("file_path") != null) {
                        metadataMap.put("file_path", originalMetadata.getString("file_path"));
                    }
                    if (originalMetadata.getString("page_number") != null) {
                        metadataMap.put("page_number", originalMetadata.getString("page_number"));
                    }
                    if (originalMetadata.getString("total_pages") != null) {
                        metadataMap.put("total_pages", originalMetadata.getString("total_pages"));
                    }

                    // 确保必要的字段存在（覆盖或添加）
                    metadataMap.put("file_name", fileName);
                    if (pageNumber != null && !pageNumber.isEmpty()) {
                        metadataMap.put("page_number", pageNumber);
                    }

                    // 使用Map创建新的Metadata对象
                    Metadata newMetadata = Metadata.from(metadataMap);

                    return TextSegment.from(enhancedText.toString(), newMetadata);
                })
                // 使用指定的向量模型
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 向量化处理文档
        if (!documents.isEmpty()) {
            try {
                // 检查是否需要重新向量化
                boolean needsVectorization = checkIfVectorizationNeeded(documents);

                if (needsVectorization) {
                    log.info("检测到文档变化，开始重新向量化处理（会消耗API额度）...");

                    // 清空现有的向量数据，避免旧数据残留
                    if (embeddingStore instanceof dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore) {
                        dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<TextSegment> inMemoryStore = (dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<TextSegment>) embeddingStore;
                        inMemoryStore.removeAll();
                        log.info("✓ 已清空旧的向量数据");
                    }

                    ingestor.ingest(documents);
                    log.info("✓ 文档向量化处理完成");

                    // 保存向量数据到文件
                    if (embeddingStore instanceof dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore) {
                        try {
                            dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<TextSegment> inMemoryStore = (dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<TextSegment>) embeddingStore;
                            inMemoryStore.serializeToFile("embedding-store.json");
                            log.info("✓ 向量数据已保存到缓存文件");

                            // 保存文档指纹用于下次对比
                            saveDocumentFingerprint(documents);
                        } catch (Exception saveEx) {
                            log.warn("保存向量数据失败（不影响功能）: {}", saveEx.getMessage());
                        }
                    }
                } else {
                    log.info("✓ 文档未变化，使用缓存的向量数据");
                }

                // 验证metadata保存情况
                log.debug("样本metadata验证：检查前5个文档的metadata信息");
                int sampleCount = Math.min(5, documents.size());
                for (int i = 0; i < sampleCount; i++) {
                    Document doc = documents.get(i);
                    Metadata metadata = doc.metadata();
                    log.debug("文档 {} metadata: file_name={}, page_number={}",
                            i + 1,
                            metadata.getString("file_name"),
                            metadata.getString("page_number"));
                }
            } catch (Exception e) {
                log.error("文档向量化处理失败，RAG功能将不可用。错误信息: {}", e.getMessage(), e);
                log.warn("请检查以下事项：");
                log.warn("1. 阿里云 DashScope API 密钥是否正确");
                log.warn("2. 账户是否欠费或状态异常");
                log.warn("3. API 服务是否可用");
                log.warn("应用将继续启动，但 RAG 功能将不可用");
            }
        }

        // 4. 自定义内容查询器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5) // 最多 5 个检索结果
                .minScore(0.75) // 过滤掉分数小于 0.75 的结果
                .build();

        log.info("RAG系统初始化完成，支持页码引用");
        return contentRetriever;
    }

    /**
     * 检查是否需要重新向量化
     * 通过对比文档指纹（文件名、大小、修改时间）来判断
     */
    private boolean checkIfVectorizationNeeded(List<Document> documents) {
        java.io.File cacheFile = new java.io.File("embedding-store.json");
        java.io.File fingerprintFile = new java.io.File("document-fingerprint.json");

        // 如果向量缓存不存在，需要向量化
        if (!cacheFile.exists()) {
            log.info("未找到向量缓存文件，需要重新向量化");
            return true;
        }

        // 如果指纹文件不存在，需要向量化
        if (!fingerprintFile.exists()) {
            log.info("未找到文档指纹文件，需要重新向量化");
            return true;
        }

        try {
            // 读取保存的指纹
            String savedFingerprint = new String(java.nio.file.Files.readAllBytes(fingerprintFile.toPath()));

            // 计算当前文档的指纹
            String currentFingerprint = calculateDocumentFingerprint(documents);

            // 对比指纹
            if (!savedFingerprint.equals(currentFingerprint)) {
                log.info("文档已变化，需要重新向量化");
                log.debug("旧指纹: {}", savedFingerprint.substring(0, Math.min(100, savedFingerprint.length())));
                log.debug("新指纹: {}", currentFingerprint.substring(0, Math.min(100, currentFingerprint.length())));
                return true;
            }

            return false;
        } catch (Exception e) {
            log.warn("读取指纹文件失败，将重新向量化: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 计算文档指纹
     * 基于文件名和文档长度生成唯一标识
     */
    private String calculateDocumentFingerprint(List<Document> documents) {
        StringBuilder fingerprint = new StringBuilder();
        for (Document doc : documents) {
            String fileName = doc.metadata().getString("file_name");
            if (fileName != null) {
                fingerprint.append(fileName).append(":");
            }
            fingerprint.append(doc.text().length()).append(";");
        }
        return fingerprint.toString();
    }

    /**
     * 保存文档指纹到文件
     */
    private void saveDocumentFingerprint(List<Document> documents) {
        try {
            String fingerprint = calculateDocumentFingerprint(documents);
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("document-fingerprint.json"),
                    fingerprint.getBytes());
            log.info("✓ 文档指纹已保存");
        } catch (Exception e) {
            log.warn("保存文档指纹失败（不影响功能）: {}", e.getMessage());
        }
    }

    /**
     * 从文件名中提取文件类型描述
     */
    private String getFileTypeFromName(String fileName) {
        if (fileName == null)
            return "未知";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "未知文件";
        }

        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "PDF文档";
            case "doc", "docx" -> "Word文档";
            case "xls", "xlsx" -> "Excel表格";
            case "ppt", "pptx" -> "PowerPoint演示文稿";
            case "md", "markdown" -> "Markdown文档";
            case "txt" -> "文本文件";
            case "html", "htm" -> "HTML网页";
            case "java" -> "Java源代码";
            case "py" -> "Python源代码";
            case "js" -> "JavaScript源代码";
            default -> extension.toUpperCase() + "文件";
        };
    }
}