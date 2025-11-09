package com.javaProgram.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文件内容读取工具类
 * 用于从VirtualFile读取文件内容
 */
public class FileContentReader {

    /**
     * 读取VirtualFile的完整内容
     * 
     * @param virtualFile 虚拟文件
     * @param project     当前项目
     * @return 文件内容，如果读取失败则返回null
     */
    @Nullable
    public static String readFileContent(@NotNull VirtualFile virtualFile, @Nullable Project project) {
        if (!virtualFile.exists() || virtualFile.isDirectory()) {
            return null;
        }

        // 检查文件大小（限制读取超大文件，例如超过500KB）
        long fileSize = virtualFile.getLength();
        if (fileSize > 500 * 1024) {
            return null; // 文件过大，不读取
        }

        try {
            // 使用ReadAction读取文件内容
            // 明确使用Computable接口以避免方法引用不明确
            return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                @Override
                public String compute() {
                    try {
                        // 优先使用Document读取（支持编码转换）
                        if (project != null) {
                            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                            if (document != null) {
                                return document.getText();
                            }
                        }

                        // 如果Document不可用，直接读取字节并转换为字符串
                        byte[] content = virtualFile.contentsToByteArray();
                        return new String(content, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查文件是否支持添加到上下文
     * 支持代码文件、配置文件、Markdown文件等
     * 
     * @param virtualFile 虚拟文件
     * @return 是否支持
     */
    public static boolean isSupportedFile(@NotNull VirtualFile virtualFile) {
        if (virtualFile.isDirectory()) {
            return false;
        }

        String fileName = virtualFile.getName().toLowerCase();
        String extension = virtualFile.getExtension();
        if (extension == null) {
            extension = "";
        }
        extension = extension.toLowerCase();

        // 支持的代码文件扩展名
        String[] codeExtensions = {
                "java", "kt", "scala", "groovy", // JVM语言
                "py", "js", "ts", "jsx", "tsx", // 脚本语言
                "cpp", "c", "h", "hpp", "cc", "cxx", // C/C++
                "go", "rs", "rb", "php", "swift", // 其他语言
                "xml", "json", "yaml", "yml", "properties", // 配置文件
                "md", "txt", "markdown", // 文档文件
                "gradle", "maven", "pom", "build", // 构建文件
                "sql", "sh", "bat", "ps1" // 其他
        };

        for (String ext : codeExtensions) {
            if (extension.equals(ext)) {
                return true;
            }
        }

        // 检查特定文件名
        String[] supportedFileNames = {
                "pom.xml", "build.gradle", "build.gradle.kts",
                "package.json", "package-lock.json",
                "requirements.txt", "Dockerfile", "docker-compose.yml",
                "README.md", "README.txt", ".gitignore"
        };

        for (String name : supportedFileNames) {
            if (fileName.equals(name.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
