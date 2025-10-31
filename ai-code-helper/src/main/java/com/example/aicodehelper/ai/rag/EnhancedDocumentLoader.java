package com.example.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 增强的文档加载器，支持多种格式包括PDF
 */
@Component
@Slf4j
public class EnhancedDocumentLoader {

    private final ApacheTikaDocumentParser tikaParser;

    public EnhancedDocumentLoader() {
        // 初始化Apache Tika解析器，支持PDF、DOC、DOCX、PPT、PPTX等多种格式
        this.tikaParser = new ApacheTikaDocumentParser();
    }

    /**
     * 加载指定目录下的所有文档，支持多种格式
     * @param documentsPath 文档目录路径
     * @return 文档列表
     */
    public List<Document> loadAllDocuments(String documentsPath) {
        List<Document> documents = new ArrayList<>();
        Path docsPath = Paths.get(documentsPath);

        if (!Files.exists(docsPath)) {
            log.warn("文档目录不存在: {}", documentsPath);
            return documents;
        }

        try {
            // 遍历目录中的所有文件
            Files.walk(docsPath)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        String fileName = filePath.getFileName().toString();
                        String fileExtension = getFileExtension(fileName);

                        log.info("正在处理文档: {} (格式: {})", fileName, fileExtension);

                        // 根据文件类型选择合适的解析方式
                        Document document = loadDocumentByType(filePath, fileExtension);
                        if (document != null) {
                            documents.add(document);
                            log.info("成功加载文档: {}, 大小: {} 字符",
                                fileName, document.text().length());
                        }
                    } catch (Exception e) {
                        log.error("加载文档失败: {} - {}", filePath, e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.error("遍历文档目录失败: {}", e.getMessage());
        }

        log.info("总共加载了 {} 个文档", documents.size());
        return documents;
    }

    /**
     * 根据文件类型加载文档
     * @param filePath 文件路径
     * @param extension 文件扩展名
     * @return 文档对象
     */
    private Document loadDocumentByType(Path filePath, String extension) {
        try {
            // 支持的文本格式 - 使用LangChain4j默认解析器
            if (isTextFormat(extension)) {
                return FileSystemDocumentLoader.loadDocument(filePath);
            }

            // 支持的二进制格式 - 使用Apache Tika解析器
            if (isTikaSupportedFormat(extension)) {
                try (InputStream inputStream = Files.newInputStream(filePath)) {
                    return tikaParser.parse(inputStream);
                }
            }

            log.warn("不支持的文件格式: {}", extension);
            return null;

        } catch (Exception e) {
            log.error("解析文档失败: {} - {}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否为文本格式
     * @param extension 文件扩展名
     * @return 是否为文本格式
     */
    private boolean isTextFormat(String extension) {
        return extension != null && (
            extension.equalsIgnoreCase("txt") ||
            extension.equalsIgnoreCase("md") ||
            extension.equalsIgnoreCase("markdown") ||
            extension.equalsIgnoreCase("html") ||
            extension.equalsIgnoreCase("htm") ||
            extension.equalsIgnoreCase("xml") ||
            extension.equalsIgnoreCase("json") ||
            extension.equalsIgnoreCase("java") ||
            extension.equalsIgnoreCase("py") ||
            extension.equalsIgnoreCase("js") ||
            extension.equalsIgnoreCase("css")
        );
    }

    /**
     * 检查是否为Apache Tika支持的格式
     * @param extension 文件扩展名
     * @return 是否为Tika支持的格式
     */
    private boolean isTikaSupportedFormat(String extension) {
        return extension != null && (
            // PDF格式
            extension.equalsIgnoreCase("pdf") ||
            // Microsoft Office格式
            extension.equalsIgnoreCase("doc") ||
            extension.equalsIgnoreCase("docx") ||
            extension.equalsIgnoreCase("xls") ||
            extension.equalsIgnoreCase("xlsx") ||
            extension.equalsIgnoreCase("ppt") ||
            extension.equalsIgnoreCase("pptx") ||
            // 其他Tika支持的格式
            extension.equalsIgnoreCase("rtf") ||
            extension.equalsIgnoreCase("odt") ||
            extension.equalsIgnoreCase("ods") ||
            extension.equalsIgnoreCase("odp") ||
            extension.equalsIgnoreCase("epub") ||
            // 压缩格式（Tika可以解析其中的文本文件）
            extension.equalsIgnoreCase("zip") ||
            extension.equalsIgnoreCase("jar")
        );
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名（不包含点号）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 获取支持的文件格式列表
     * @return 支持的格式说明
     */
    public String getSupportedFormats() {
        return """
            支持的文档格式：

            📄 文本格式：
            - TXT, MD (Markdown), HTML, XML
            - JSON, JAVA, PY, JS, CSS 等代码文件

            📄 二进制格式（通过Apache Tika解析）：
            - PDF文档
            - Microsoft Office: DOC, DOCX, XLS, XLSX, PPT, PPTX
            - OpenDocument: ODT, ODS, ODP
            - RTF, EPUB
            - 压缩文件: ZIP, JAR（会提取其中的文本文件）
            """;
    }
}