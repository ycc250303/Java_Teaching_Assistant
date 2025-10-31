package com.example.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public ContentRetriever contentRetriever() {
        // ------ 增强版RAG ------
        log.info("开始初始化RAG系统...");

        // 1. 使用增强的文档加载器加载所有支持格式的文档
        String documentsPath = "src/main/resources/docs";
        log.info("正在从目录加载文档: {}", documentsPath);
        log.info("支持的格式: {}", enhancedDocumentLoader.getSupportedFormats());

        List<Document> documents = enhancedDocumentLoader.loadAllDocuments(documentsPath);

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
                // 为了提高搜索质量，为每个 TextSegment 添加文档名称和类型
                .textSegmentTransformer(textSegment -> {
                    String fileName = textSegment.metadata().getString("file_name");
                    String fileType = getFileTypeFromName(fileName);
                    String enhancedText = String.format("[%s - %s]\n%s", fileName, fileType, textSegment.text());
                    return TextSegment.from(enhancedText, textSegment.metadata());
                })
                // 使用指定的向量模型
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 向量化处理文档
        if (!documents.isEmpty()) {
            ingestor.ingest(documents);
            log.info("文档向量化处理完成");
        }

        // 4. 自定义内容查询器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5) // 最多 5 个检索结果
                .minScore(0.75) // 过滤掉分数小于 0.75 的结果
                .build();

        log.info("RAG系统初始化完成");
        return contentRetriever;
    }

    /**
     * 从文件名中提取文件类型描述
     */
    private String getFileTypeFromName(String fileName) {
        if (fileName == null) return "未知";

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
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