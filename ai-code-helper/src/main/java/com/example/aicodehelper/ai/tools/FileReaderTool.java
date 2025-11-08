package com.example.aicodehelper.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件读取工具 - 让AI能够自主读取项目代码
 * 
 * 功能：
 * 1. 列出项目目录结构
 * 2. 读取指定文件的内容
 * 3. 在项目中搜索包含特定文本的文件
 */
@Slf4j
public class FileReaderTool {

    private String projectRootPath;

    /**
     * 构造函数
     * 
     * @param projectRootPath 项目根目录路径
     */
    public FileReaderTool(String projectRootPath) {
        this.projectRootPath = projectRootPath;
        log.info("FileReaderTool initialized with project root: {}", projectRootPath);
    }

    /**
     * 更新项目根目录路径（由前端在用户提问时动态设置）
     */
    public void setProjectRootPath(String projectRootPath) {
        this.projectRootPath = projectRootPath;
        log.info("Project root path updated to: {}", projectRootPath);
    }

    /**
     * 列出指定目录下的文件和子目录
     * 
     * @param relativePath 相对于项目根目录的路径（例如："src/main/java"）
     * @return 文件和目录列表，格式化为易读的字符串
     */
    @Tool(name = "listProjectFiles", value = """
            Lists all files and directories in the specified project directory.
            Use this when you need to understand the project structure or find specific files.
            Input should be a relative path from the project root (e.g., "src/main/java", "src/main/resources").
            Leave empty to list the root directory.
            """)
    public String listProjectFiles(@P(value = "relative path from project root") String relativePath) {
        try {
            // 如果 relativePath 为 null 或空，使用项目根目录
            String targetPath = (relativePath == null || relativePath.trim().isEmpty())
                    ? projectRootPath
                    : Paths.get(projectRootPath, relativePath).toString();

            File directory = new File(targetPath);

            if (!directory.exists()) {
                return "❌ 目录不存在: " + relativePath;
            }

            if (!directory.isDirectory()) {
                return "❌ 指定的路径不是目录: " + relativePath;
            }

            File[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                return "📁 目录为空: " + relativePath;
            }

            StringBuilder result = new StringBuilder();
            result.append("📂 目录内容: ").append(relativePath.isEmpty() ? "/" : relativePath).append("\n\n");

            // 分别列出目录和文件
            List<String> directories = new ArrayList<>();
            List<String> regularFiles = new ArrayList<>();

            for (File file : files) {
                // 跳过隐藏文件和特定目录
                if (file.getName().startsWith(".") ||
                        file.getName().equals("target") ||
                        file.getName().equals("build") ||
                        file.getName().equals("node_modules")) {
                    continue;
                }

                if (file.isDirectory()) {
                    directories.add("📁 " + file.getName() + "/");
                } else {
                    long fileSizeKB = file.length() / 1024;
                    regularFiles.add("📄 " + file.getName() + " (" + fileSizeKB + " KB)");
                }
            }

            // 先输出目录，再输出文件
            directories.forEach(dir -> result.append(dir).append("\n"));
            regularFiles.forEach(f -> result.append(f).append("\n"));

            result.append("\n共 ").append(directories.size()).append(" 个目录，")
                    .append(regularFiles.size()).append(" 个文件");

            return result.toString();

        } catch (Exception e) {
            log.error("Error listing files in path: {}", relativePath, e);
            return "❌ 读取目录失败: " + e.getMessage();
        }
    }

    /**
     * 读取指定文件的完整内容
     * 
     * @param filePath 相对于项目根目录的文件路径（例如："src/main/java/com/example/Main.java"）
     * @return 文件内容，带行号
     */
    @Tool(name = "readProjectFile", value = """
            Reads the complete content of a specified file in the project.
            Use this when you need to analyze, understand, or modify specific code files.
            Input should be a relative file path from the project root.
            The output will include line numbers for easy reference.
            """)
    public String readProjectFile(@P(value = "relative file path from project root") String filePath) {
        try {
            Path fullPath = Paths.get(projectRootPath, filePath);
            File file = fullPath.toFile();

            if (!file.exists()) {
                return "❌ 文件不存在: " + filePath;
            }

            if (!file.isFile()) {
                return "❌ 指定的路径不是文件: " + filePath;
            }

            // 检查文件大小（限制读取超大文件）
            long fileSizeKB = file.length() / 1024;
            if (fileSizeKB > 500) {
                return "❌ 文件过大 (" + fileSizeKB + " KB)，建议使用更具体的搜索或指定行号范围";
            }

            // 读取文件内容
            List<String> lines = Files.readAllLines(fullPath);

            StringBuilder result = new StringBuilder();
            result.append("📄 文件: ").append(filePath).append("\n");
            result.append("📏 总行数: ").append(lines.size()).append("\n");
            result.append("─".repeat(50)).append("\n\n");

            // 添加行号
            for (int i = 0; i < lines.size(); i++) {
                result.append(String.format("%4d | %s\n", i + 1, lines.get(i)));
            }

            return result.toString();

        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
            return "❌ 读取文件失败: " + e.getMessage();
        }
    }

    /**
     * 在项目中搜索包含特定文本的文件
     * 
     * @param searchText    要搜索的文本（支持类名、方法名、变量名等）
     * @param fileExtension 文件扩展名过滤（例如：".java", ".xml"），留空则搜索所有文件
     * @return 包含该文本的文件列表及匹配的行
     */
    @Tool(name = "searchCodeInProject", value = """
            Searches for files containing specific text in the project.
            Useful for finding where a class, method, or variable is defined or used.
            Specify the search text and optionally a file extension to narrow down results.
            Example: searchText="UserService", fileExtension=".java"
            """)
    public String searchCodeInProject(
            @P(value = "text to search for") String searchText,
            @P(value = "file extension filter (e.g., '.java', '.xml'), leave empty for all files") String fileExtension) {
        try {
            Path rootPath = Paths.get(projectRootPath);
            List<SearchResult> results = new ArrayList<>();

            // 遍历项目文件
            try (Stream<Path> pathStream = Files.walk(rootPath)) {
                pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String pathStr = path.toString();
                            // 跳过不相关的目录
                            if (pathStr.contains("\\.git\\") || pathStr.contains("\\target\\") ||
                                    pathStr.contains("\\build\\") || pathStr.contains("\\node_modules\\")) {
                                return false;
                            }
                            // 过滤文件扩展名
                            if (fileExtension != null && !fileExtension.trim().isEmpty()) {
                                return pathStr.endsWith(fileExtension);
                            }
                            return true;
                        })
                        .forEach(path -> {
                            try {
                                List<String> lines = Files.readAllLines(path);
                                for (int i = 0; i < lines.size(); i++) {
                                    if (lines.get(i).contains(searchText)) {
                                        String relativePath = rootPath.relativize(path).toString();
                                        results.add(new SearchResult(relativePath, i + 1, lines.get(i).trim()));
                                    }
                                }
                            } catch (IOException e) {
                                // 跳过无法读取的文件
                            }
                        });
            }

            if (results.isEmpty()) {
                return "🔍 未找到包含 \"" + searchText + "\" 的代码";
            }

            // 限制结果数量
            int maxResults = 20;
            StringBuilder result = new StringBuilder();
            result.append("🔍 搜索结果 (\"").append(searchText).append("\")\n");
            result.append("找到 ").append(results.size()).append(" 处匹配");
            if (results.size() > maxResults) {
                result.append("，仅显示前 ").append(maxResults).append(" 条");
            }
            result.append("\n\n");

            for (int i = 0; i < Math.min(maxResults, results.size()); i++) {
                SearchResult sr = results.get(i);
                result.append(String.format("📄 %s (第%d行)\n", sr.filePath, sr.lineNumber));
                result.append("   > ").append(sr.lineContent).append("\n\n");
            }

            return result.toString();

        } catch (IOException e) {
            log.error("Error searching code in project", e);
            return "❌ 搜索失败: " + e.getMessage();
        }
    }

    /**
     * 搜索结果数据类
     */
    private static class SearchResult {
        String filePath;
        int lineNumber;
        String lineContent;

        SearchResult(String filePath, int lineNumber, String lineContent) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
        }
    }
}
