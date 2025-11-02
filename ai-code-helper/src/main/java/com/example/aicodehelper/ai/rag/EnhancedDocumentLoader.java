package com.example.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强的文档加载器，支持多种格式包括PDF，并支持PDF页码提取
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
            try (var stream = Files.walk(docsPath)) {
                stream.filter(Files::isRegularFile)
                        .forEach(filePath -> {
                            try {
                                String fileName = filePath.getFileName().toString();
                                String fileExtension = getFileExtension(fileName);

                                log.info("正在处理文档: {} (格式: {})", fileName, fileExtension);

                                // 根据文件类型选择合适的解析方式
                                List<Document> docs = loadDocumentByType(filePath, fileExtension);
                                if (docs != null && !docs.isEmpty()) {
                                    documents.addAll(docs);
                                    log.info("成功加载文档: {}, 共 {} 个文档片段", fileName, docs.size());
                                }
                            } catch (Exception e) {
                                log.error("加载文档失败: {} - {}", filePath, e.getMessage(), e);
                            }
                        });
            }
        } catch (IOException e) {
            log.error("遍历文档目录失败: {}", e.getMessage(), e);
        }

        log.info("总共加载了 {} 个文档片段", documents.size());
        return documents;
    }

    /**
     * 根据文件类型加载文档
     * @param filePath 文件路径
     * @param extension 文件扩展名
     * @return 文档对象列表（PDF文件会按页分割，返回多个Document）
     */
    private List<Document> loadDocumentByType(Path filePath, String extension) {
        try {
            String fileName = filePath.getFileName().toString();

            // PDF文件特殊处理：按页提取并标注页码
            if ("pdf".equalsIgnoreCase(extension)) {
                return loadPdfWithPageNumbers(filePath, fileName);
            }

            // 支持的文本格式 - 使用LangChain4j默认解析器
            if (isTextFormat(extension)) {
                Document doc = FileSystemDocumentLoader.loadDocument(filePath);
                // 添加文件名metadata
                Document documentWithMetadata = addMetadataToDocument(doc, fileName, filePath.toString());
                return List.of(documentWithMetadata);
            }

            // 支持的二进制格式 - 使用Apache Tika解析器
            if (isTikaSupportedFormat(extension)) {
                try (InputStream inputStream = Files.newInputStream(filePath)) {
                    Document doc = tikaParser.parse(inputStream);
                    // 添加文件名metadata
                    Document documentWithMetadata = addMetadataToDocument(doc, fileName, filePath.toString());
                    return List.of(documentWithMetadata);
                }
            }

            log.warn("不支持的文件格式: {}", extension);
            return null;

        } catch (Exception e) {
            log.error("解析文档失败: {} - {}", filePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 为Document添加metadata
     * @param doc 原始Document
     * @param fileName 文件名
     * @param filePath 文件路径
     * @return 带有metadata的Document
     */
    private Document addMetadataToDocument(Document doc, String fileName, String filePath) {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("file_name", fileName);
        metadataMap.put("file_path", filePath);
        Metadata metadata = Metadata.from(metadataMap);
        return Document.from(doc.text(), metadata);
    }

    /**
     * 加载PDF文件并按页提取文本，为每页创建独立的Document并标注页码
     * @param filePath PDF文件路径
     * @param fileName 文件名
     * @return Document列表，每个Document对应一页
     */
    private List<Document> loadPdfWithPageNumbers(Path filePath, String fileName) {
        List<Document> documents = new ArrayList<>();

        try (PDDocument pdDocument = Loader.loadPDF(filePath.toFile())) {
            int totalPages = pdDocument.getNumberOfPages();
            log.info("PDF文件 {} 共有 {} 页", fileName, totalPages);

            PDFTextStripper textStripper = new PDFTextStripper();

            // 逐页提取文本
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                try {
                    // 设置提取范围：只提取当前页
                    textStripper.setStartPage(pageNum);
                    textStripper.setEndPage(pageNum);

                    // 提取当前页文本
                    String pageText = textStripper.getText(pdDocument);

                    // 如果页面为空或只有空白字符，跳过
                    if (pageText == null || pageText.trim().isEmpty()) {
                        log.debug("第 {} 页为空，跳过", pageNum);
                        continue;
                    }

                    // 创建包含页码信息的metadata
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("file_name", fileName);
                    metadataMap.put("file_path", filePath.toString());
                    metadataMap.put("page_number", String.valueOf(pageNum));
                    metadataMap.put("total_pages", String.valueOf(totalPages));

                    // 创建Document对象
                    Metadata metadata = Metadata.from(metadataMap);
                    Document pageDocument = Document.from(pageText.trim(), metadata);
                    documents.add(pageDocument);

                    log.debug("已提取第 {} 页，文本长度: {} 字符", pageNum, pageText.length());
                } catch (Exception e) {
                    log.error("提取PDF第 {} 页失败: {}", pageNum, e.getMessage());
                    // 继续处理下一页
                }
            }

            log.info("PDF文件 {} 处理完成，共提取 {} 页有效内容", fileName, documents.size());
        } catch (IOException e) {
            log.error("加载PDF文件失败: {} - {}", filePath, e.getMessage(), e);
            // 如果PDFBox解析失败，降级使用Tika解析（但不包含页码信息）
            log.warn("尝试使用Tika降级解析...");
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                Document doc = tikaParser.parse(inputStream);
                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put("file_name", fileName);
                metadataMap.put("file_path", filePath.toString());
                Metadata metadata = Metadata.from(metadataMap);
                Document documentWithMetadata = Document.from(doc.text(), metadata);
                return List.of(documentWithMetadata);
            } catch (Exception ex) {
                log.error("Tika降级解析也失败: {}", ex.getMessage(), ex);
            }
        }

        return documents;
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
                // PDF格式（现在会特殊处理，但保留在列表中以便降级）
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
            - PDF文档（支持页码提取）
            - Microsoft Office: DOC, DOCX, XLS, XLSX, PPT, PPTX
            - OpenDocument: ODT, ODS, ODP
            - RTF, EPUB
            - 压缩文件: ZIP, JAR（会提取其中的文本文件）
            """;
    }
}