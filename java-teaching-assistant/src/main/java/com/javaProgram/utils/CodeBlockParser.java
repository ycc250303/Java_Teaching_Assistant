package com.javaProgram.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码块解析器
 * 用于从粘贴的文本中识别和提取代码块信息
 */
public class CodeBlockParser {
    public static class CodeBlock {
        public String filePath; // 文件路径
        public String fileName; // 文件名
        public String code; // 代码内容
        public int startLine; // 起始行号
        public int endLine; // 结束行号
        public String language; // 编程语言

        public boolean isValid() {
            return code != null && !code.trim().isEmpty();
        }
    }

    // 匹配 IntelliJ 复制的代码格式（带文件路径和行号）
    // 格式示例：
    // D:\path\to\File.java:10-20
    // public class Example {
    // ...
    // }
    private static final Pattern INTELLIJ_PATTERN = Pattern.compile(
            "^([A-Za-z]:[\\\\\\/.][^:]+\\.(java|kt|py|js|ts|cpp|c|h|go|rs|rb|php|swift|xml|json|yaml|yml|properties|gradle|xml)):?(\\d+)?-?(\\d+)?\\s*\\n([\\s\\S]+)",
            Pattern.MULTILINE);

    // 匹配简单的文件路径格式
    // 格式示例：
    // File.java
    // public class Example { ... }
    private static final Pattern SIMPLE_FILE_PATTERN = Pattern.compile(
            "^([A-Za-z_][A-Za-z0-9_]*\\.(java|kt|py|js|ts|cpp|c|h|go|rs))\\s*\\n([\\s\\S]+)",
            Pattern.MULTILINE);

    // 判断是否为代码块的启发式规则
    private static final Pattern CODE_HEURISTICS = Pattern.compile(
            "(class\\s+\\w+|public\\s+|private\\s+|protected\\s+|function\\s+|def\\s+|import\\s+|package\\s+|\\{|\\}|;$|//|/\\*|\\*/)",
            Pattern.MULTILINE);

    /**
     * 解析粘贴的文本，尝试提取代码块信息
     * 
     * @param text    粘贴的文本
     * @param project 当前项目
     * @return 代码块信息，如果不是代码块则返回null
     */
    public static CodeBlock parse(String text, Project project) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // 1. 尝试匹配 IntelliJ IDEA 格式（带完整路径）
        Matcher intellijMatcher = INTELLIJ_PATTERN.matcher(text);
        if (intellijMatcher.find()) {
            return parseIntelliJFormat(intellijMatcher);
        }

        // 2. 尝试匹配简单文件名格式
        Matcher simpleMatcher = SIMPLE_FILE_PATTERN.matcher(text);
        if (simpleMatcher.find()) {
            return parseSimpleFormat(simpleMatcher);
        }

        // 3. 启发式判断是否为代码（没有文件路径的纯代码）
        if (isLikelyCode(text)) {
            return parseRawCode(text);
        }

        return null;
    }

    /**
     * 解析 IntelliJ IDEA 格式的代码块
     */
    private static CodeBlock parseIntelliJFormat(Matcher matcher) {
        CodeBlock block = new CodeBlock();
        block.filePath = matcher.group(1);
        String extension = matcher.group(2);
        String startLineStr = matcher.group(3);
        String endLineStr = matcher.group(4);
        block.code = matcher.group(5);

        // 提取文件名
        block.fileName = block.filePath.substring(
                Math.max(block.filePath.lastIndexOf('/'), block.filePath.lastIndexOf('\\')) + 1);

        // 解析行号
        if (startLineStr != null) {
            block.startLine = Integer.parseInt(startLineStr);
        }
        if (endLineStr != null) {
            block.endLine = Integer.parseInt(endLineStr);
        }

        // 推断语言
        block.language = inferLanguage(extension);

        return block;
    }

    /**
     * 解析简单格式（只有文件名）
     */
    private static CodeBlock parseSimpleFormat(Matcher matcher) {
        CodeBlock block = new CodeBlock();
        block.fileName = matcher.group(1);
        String extension = matcher.group(2);
        block.code = matcher.group(3);
        block.language = inferLanguage(extension);
        return block;
    }

    /**
     * 解析纯代码（没有文件信息）
     */
    private static CodeBlock parseRawCode(String text) {
        CodeBlock block = new CodeBlock();
        block.fileName = "code_snippet";
        block.code = text;
        block.language = inferLanguageFromCode(text);
        return block;
    }

    /**
     * 启发式判断文本是否像代码
     */
    private static boolean isLikelyCode(String text) {
        // 文本太短，不太可能是代码
        if (text.length() < 20) {
            return false;
        }

        // 检查是否包含代码特征
        Matcher matcher = CODE_HEURISTICS.matcher(text);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
            if (matchCount >= 2) { // 至少匹配2个代码特征
                return true;
            }
        }

        // 检查是否有多行且包含缩进
        String[] lines = text.split("\n");
        if (lines.length >= 3) {
            int indentedLines = 0;
            for (String line : lines) {
                if (line.startsWith("    ") || line.startsWith("\t")) {
                    indentedLines++;
                }
            }
            // 如果超过30%的行有缩进，可能是代码
            if (indentedLines > lines.length * 0.3) {
                return true;
            }
        }

        return false;
    }

    /**
     * 根据文件扩展名推断语言
     */
    private static String inferLanguage(String extension) {
        switch (extension.toLowerCase()) {
            case "java":
                return "Java";
            case "kt":
                return "Kotlin";
            case "py":
                return "Python";
            case "js":
                return "JavaScript";
            case "ts":
                return "TypeScript";
            case "cpp":
            case "cc":
            case "cxx":
                return "C++";
            case "c":
                return "C";
            case "h":
            case "hpp":
                return "C/C++ Header";
            case "go":
                return "Go";
            case "rs":
                return "Rust";
            case "rb":
                return "Ruby";
            case "php":
                return "PHP";
            case "swift":
                return "Swift";
            case "xml":
                return "XML";
            case "json":
                return "JSON";
            case "yaml":
            case "yml":
                return "YAML";
            default:
                return "Unknown";
        }
    }

    /**
     * 从代码内容推断语言
     */
    private static String inferLanguageFromCode(String code) {
        if (code.contains("public class") || code.contains("package ")) {
            return "Java";
        } else if (code.contains("def ") && code.contains(":")) {
            return "Python";
        } else if (code.contains("function ") || code.contains("const ") || code.contains("let ")) {
            return "JavaScript";
        } else if (code.contains("#include")) {
            return "C/C++";
        } else if (code.contains("func ")) {
            return "Go";
        }
        return "Unknown";
    }
}
