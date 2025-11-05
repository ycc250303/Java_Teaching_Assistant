package com.example.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnhancedDocumentLoader 测试类
 * 测试文档加载功能
 */
@DisplayName("增强文档加载器测试")
class EnhancedDocumentLoaderTest {

    private EnhancedDocumentLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new EnhancedDocumentLoader();
    }

    @Test
    @DisplayName("测试加载不存在的目录")
    void testLoadNonExistentDirectory() {
        // 测试加载不存在的目录
        List<Document> documents = loader.loadAllDocuments("/non/existent/path");

        // 应该返回空列表而不是抛出异常
        assertNotNull(documents);
        assertTrue(documents.isEmpty(), "加载不存在的目录应该返回空列表");
    }

    @Test
    @DisplayName("测试加载空目录")
    void testLoadEmptyDirectory() throws IOException {
        // 创建一个空的临时目录
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        // 测试加载空目录
        List<Document> documents = loader.loadAllDocuments(emptyDir.toString());

        assertNotNull(documents);
        assertTrue(documents.isEmpty(), "加载空目录应该返回空列表");
    }

    @Test
    @DisplayName("测试加载文本文件")
    void testLoadTextFile() throws IOException {
        // 创建一个测试文本文件
        Path testFile = tempDir.resolve("test.txt");
        String content = "这是一个测试文件\nJava编程测试";
        Files.writeString(testFile, content);

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证
        assertNotNull(documents);
        assertEquals(1, documents.size(), "应该加载1个文档");

        Document doc = documents.get(0);
        assertTrue(doc.text().contains("测试文件"), "文档内容应该包含原始文本");

        // 验证 metadata
        Metadata metadata = doc.metadata();
        assertNotNull(metadata);
        assertEquals("test.txt", metadata.getString("file_name"),
                "应该包含文件名 metadata");
    }

    @Test
    @DisplayName("测试加载 Markdown 文件")
    void testLoadMarkdownFile() throws IOException {
        // 创建 Markdown 文件
        Path mdFile = tempDir.resolve("test.md");
        String content = "# 标题\n\n这是 Markdown 内容\n\n## 子标题";
        Files.writeString(mdFile, content);

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertTrue(documents.get(0).text().contains("标题"));
    }

    @Test
    @DisplayName("测试加载 Java 源代码文件")
    void testLoadJavaFile() throws IOException {
        // 创建 Java 文件
        Path javaFile = tempDir.resolve("Example.java");
        String content = """
                public class Example {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
                """;
        Files.writeString(javaFile, content);

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertTrue(documents.get(0).text().contains("public class Example"));
    }

    @Test
    @DisplayName("测试加载多个文件")
    void testLoadMultipleFiles() throws IOException {
        // 创建多个测试文件
        Files.writeString(tempDir.resolve("file1.txt"), "内容1");
        Files.writeString(tempDir.resolve("file2.md"), "# 内容2");
        Files.writeString(tempDir.resolve("file3.java"), "public class Test {}");

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证
        assertNotNull(documents);
        assertEquals(3, documents.size(), "应该加载3个文档");
    }

    @Test
    @DisplayName("测试文件扩展名识别")
    void testFileExtensionRecognition() throws IOException {
        // 创建不同类型的文件
        Files.writeString(tempDir.resolve("test.TXT"), "大写扩展名");
        Files.writeString(tempDir.resolve("test.Md"), "混合大小写");

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证应该正确识别大小写不敏感的扩展名
        assertNotNull(documents);
        assertTrue(documents.size() >= 2, "应该识别大小写不敏感的扩展名");
    }

    @Test
    @DisplayName("测试忽略不支持的文件格式")
    void testIgnoreUnsupportedFormat() throws IOException {
        // 创建不支持的文件格式
        Files.writeString(tempDir.resolve("test.xyz"), "未知格式");
        Files.writeString(tempDir.resolve("test.txt"), "支持的格式");

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证只加载支持的格式
        assertNotNull(documents);
        assertEquals(1, documents.size(), "应该只加载支持的文件格式");
        assertEquals("test.txt", documents.get(0).metadata().getString("file_name"));
    }

    @Test
    @DisplayName("测试 metadata 完整性")
    void testMetadataCompleteness() throws IOException {
        // 创建测试文件
        Path testFile = tempDir.resolve("metadata-test.txt");
        Files.writeString(testFile, "测试 metadata");

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证 metadata
        assertNotNull(documents);
        assertEquals(1, documents.size());

        Metadata metadata = documents.get(0).metadata();
        assertNotNull(metadata);

        // 验证必要的 metadata 字段
        assertNotNull(metadata.getString("file_name"), "应该包含文件名");
        assertNotNull(metadata.getString("file_path"), "应该包含文件路径");
        assertEquals("metadata-test.txt", metadata.getString("file_name"));
    }

    @Test
    @DisplayName("测试获取支持的格式列表")
    void testGetSupportedFormats() {
        // 获取支持的格式说明
        String formats = loader.getSupportedFormats();

        // 验证
        assertNotNull(formats);
        assertFalse(formats.isEmpty());
        assertTrue(formats.contains("TXT"), "应该包含 TXT 格式");
        assertTrue(formats.contains("PDF"), "应该包含 PDF 格式");
        assertTrue(formats.contains("JAVA"), "应该包含 JAVA 格式");
    }

    @Test
    @DisplayName("测试子目录文件加载")
    void testLoadFilesInSubdirectories() throws IOException {
        // 创建子目录结构
        Path subDir1 = tempDir.resolve("subdir1");
        Path subDir2 = tempDir.resolve("subdir2");
        Files.createDirectory(subDir1);
        Files.createDirectory(subDir2);

        // 在不同目录创建文件
        Files.writeString(tempDir.resolve("root.txt"), "根目录文件");
        Files.writeString(subDir1.resolve("sub1.txt"), "子目录1文件");
        Files.writeString(subDir2.resolve("sub2.txt"), "子目录2文件");

        // 加载文档
        List<Document> documents = loader.loadAllDocuments(tempDir.toString());

        // 验证应该递归加载所有子目录的文件
        assertNotNull(documents);
        assertEquals(3, documents.size(), "应该递归加载所有子目录的文件");
    }

    @Test
    @DisplayName("测试加载实际的课程 PDF 文件")
    void testLoadActualCoursePDFs() {
        // 这个测试需要实际的 PDF 文件
        // 如果 docs 目录存在，测试加载
        String docsPath = "src/main/resources/docs";
        Path path = Path.of(docsPath);

        if (Files.exists(path)) {
            List<Document> documents = loader.loadAllDocuments(docsPath);

            assertNotNull(documents);
            System.out.println("成功加载 " + documents.size() + " 个文档片段");

            // 验证 PDF 文档包含页码信息
            documents.stream()
                    .filter(doc -> doc.metadata().getString("file_name") != null
                            && doc.metadata().getString("file_name").endsWith(".pdf"))
                    .findFirst()
                    .ifPresent(doc -> {
                        Metadata metadata = doc.metadata();
                        System.out.println("PDF metadata: " +
                                "file=" + metadata.getString("file_name") +
                                ", page=" + metadata.getString("page_number"));

                        // PDF 文件应该包含页码信息
                        assertNotNull(metadata.getString("page_number"),
                                "PDF 文档应该包含页码信息");
                    });
        } else {
            System.out.println("跳过实际 PDF 测试：docs 目录不存在");
        }
    }
}
