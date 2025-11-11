package com.javaProgram.utils;

import com.intellij.ui.JBColor;
import java.awt.Color;
import java.util.regex.Pattern;

/**
 * Markdown转HTML工具类
 * 将Markdown语法转换为简单的HTML格式，用于在JEditorPane中显示
 */
public class MarkdownToHtml {

    /**
     * 将Markdown文本转换为HTML格式（使用默认颜色）
     * 
     * @param markdown Markdown格式的文本
     * @return 转换后的HTML文本
     */
    public static String convert(String markdown) {
        return convert(markdown, null);
    }

    /**
     * 将Markdown文本转换为HTML格式
     * 
     * @param markdown  Markdown格式的文本
     * @param textColor 文本颜色，如果为null则使用默认颜色
     * @return 转换后的HTML文本
     */
    public static String convert(String markdown, Color textColor) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // 获取代码块背景色（深色主题下使用深灰色，浅色主题下使用浅灰色）
        Color codeBlockBg = getCodeBlockBackground();
        String codeBlockBgHex = colorToHex(codeBlockBg);

        // 获取代码文本颜色（使用传入的文本颜色，如果没有则使用默认值）
        String codeTextColorHex = textColor != null ? colorToHex(textColor) : "#cccccc";

        String html = markdown;

        // 使用占位符保存代码块，避免后续处理影响代码块内容
        java.util.Map<String, String> codeBlockPlaceholders = new java.util.HashMap<>();
        int placeholderIndex = 0;

        // 1. 先处理代码块 ```language ... ```，用占位符替换
        // 修复正则表达式，确保正确捕获代码内容和换行符
        // 移除结束标记前的强制换行要求，避免吞掉代码内容
        Pattern codeBlockPattern = Pattern.compile("```(?:(java|javascript|python|cpp|c|go|rust|sql|html|css|json|xml|yaml|yml|sh|bash|php|ruby|swift|kotlin|scala|typescript|markdown|md))?\\s*([\\s\\S]*?)```", Pattern.MULTILINE);
        java.util.regex.Matcher codeBlockMatcher = codeBlockPattern.matcher(html);
        StringBuffer codeBlockResult = new StringBuffer();
        while (codeBlockMatcher.find()) {
            String language = codeBlockMatcher.group(1); // 语言标识符（可能为null）
            String code = codeBlockMatcher.group(2);     // 代码内容

            // 调试信息：检查代码是否被正确捕获
            if (code != null && code.length() > 0) {
                // 检查代码开头是否被意外截断
                if (code.startsWith("\n")) {
                    code = code.substring(1); // 移除开头的换行符
                }
            }
            // 转义代码块内的HTML特殊字符
            code = escapeHtml(code);
            String placeholder = "___CODE_BLOCK_" + placeholderIndex + "___";
            // 修改代码块HTML结构，确保完整的矩形背景和智能缩进显示
            // 智能添加缩进：如果代码缺少缩进，尝试自动添加
            String processedCode = addSmartIndentation(code);

            codeBlockPlaceholders.put(placeholder,
                    "<table width='100%' cellpadding='0' cellspacing='0' style='margin: 8px 0;'>"
                    + "<tr><td style='background-color: " + codeBlockBgHex + "; border: 1px solid "
                    + darkenColorHex(codeBlockBgHex, 0.2) + "; padding: 10px; font-family: Consolas, Monaco, \"Courier New\", monospace; font-size: 12px; color: "
                    + codeTextColorHex + "; white-space: pre; overflow-x: auto; tab-size: 4; -moz-tab-size: 4; word-break: normal; -webkit-hyphens: none; -moz-hyphens: none; hyphens: none;'>"
                    + processedCode
                    + "</td></tr></table>");
            codeBlockMatcher.appendReplacement(codeBlockResult, placeholder);
            placeholderIndex++;
        }
        codeBlockMatcher.appendTail(codeBlockResult);
        html = codeBlockResult.toString();

        // 2. 先处理标题（在转义HTML之前，避免标题被转义）
        // 按从多到少的顺序处理，确保不会互相干扰
        html = html.replaceAll("(?m)^##### (.+)$",
                "<h5 style='margin: 10px 0 6px 0; font-size: 14px; font-weight: bold;'>$1</h5>");
        html = html.replaceAll("(?m)^#### (.+)$",
                "<h4 style='margin: 11px 0 7px 0; font-size: 15px; font-weight: bold;'>$1</h4>");
        html = html.replaceAll("(?m)^### (.+)$",
                "<h3 style='margin: 12px 0 8px 0; font-size: 16px; font-weight: bold;'>$1</h3>");
        html = html.replaceAll("(?m)^## (.+)$",
                "<h2 style='margin: 14px 0 10px 0; font-size: 18px; font-weight: bold;'>$1</h2>");
        html = html.replaceAll("(?m)^# (.+)$",
                "<h1 style='margin: 16px 0 12px 0; font-size: 20px; font-weight: bold;'>$1</h1>");

        // 3. 转义HTML特殊字符（在代码块和标题处理之后，但占位符不会被转义因为不包含特殊字符）
        html = escapeHtml(html);

        // 4. 恢复代码块占位符（在转义之后恢复，这样代码块内容不会被再次转义）
        for (java.util.Map.Entry<String, String> entry : codeBlockPlaceholders.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        // 5. 处理行内代码 `code`（需要在代码块之后处理，避免冲突，移除可能不兼容的CSS）
        html = html.replaceAll("`([^`]+)`", "<code style='background-color: " + codeBlockBgHex + "; color: "
                + codeTextColorHex + "; padding: 2px 4px; font-family: monospace; font-size: 12px;'>$1</code>");

        // 6. 处理粗体 **text** 或 __text__
        html = html.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("__(?!_)([^_]+)__(?!_)", "<strong>$1</strong>");

        // 7. 处理斜体 *text* 或 _text_（需要避免与粗体和列表冲突）
        html = html.replaceAll("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)", "<em>$1</em>");
        html = html.replaceAll("(?<!_)_([^_\\n]+?)_(?!_)", "<em>$1</em>");

        // 8. 处理无序列表 - 或 *
        html = html.replaceAll("(?m)^[-*] (.+)$", "<li style='margin: 4px 0;'>$1</li>");
        // 将连续的列表项包裹在ul标签中
        html = wrapListItems(html);

        // 9. 处理有序列表 1. 2. 3.
        html = html.replaceAll("(?m)^\\d+\\. (.+)$", "<li style='margin: 4px 0;'>$1</li>");
        // 将有序列表项也包裹在ul标签中（简化处理）
        html = wrapListItems(html);

        // 10. 处理引用 >
        html = html.replaceAll("(?m)^> (.+)$",
                "<blockquote style='border-left: 3px solid #ddd; padding-left: 12px; margin: 8px 0; color: #666;'>$1</blockquote>");

        // 11. 处理链接 [text](url)（移除text-decoration，可能不兼容）
        html = html.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href='$2' style='color: #0066cc;'>$1</a>");

        // 12. 处理段落分隔和换行（在最后处理，确保不影响其他元素）
        html = processParagraphs(html, codeBlockPlaceholders);

        // 13. 包裹在div中，设置基本样式（简化样式，提高兼容性）
        // 获取文本颜色（RGB十六进制）
        String colorHex = textColor != null ? colorToHex(textColor) : "#333333";
        html = "<div style='font-family: Arial; font-size: 14px; color: " + colorHex
                + "; padding: 8px;'>"
                + html + "</div>";

        return html;
    }

    /**
     * 将Color转换为十六进制颜色字符串
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 转义HTML特殊字符
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 将连续的列表项包裹在ul标签中
     */
    private static String wrapListItems(String html) {
        // 找到连续的<li>标签并包裹
        Pattern listPattern = Pattern.compile("(<li[^>]*>.*?</li>(?:\\s*<li[^>]*>.*?</li>)*)", Pattern.DOTALL);
        java.util.regex.Matcher matcher = listPattern.matcher(html);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String listItems = matcher.group(1);
            String wrapped = "<ul style='margin: 8px 0; padding-left: 24px;'>" + listItems + "</ul>";
            matcher.appendReplacement(result, wrapped);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理段落分隔和换行
     * 将Markdown中的空行（两个或更多换行符）转换为段落分隔
     * 将单个换行符转换为<br/>
     * 标签
     * 保护已处理的块级元素（标题、列表、引用、代码块等）
     */
    private static String processParagraphs(String html, java.util.Map<String, String> codeBlockPlaceholders) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }

        // 使用占位符保护块级元素，避免段落处理影响它们
        java.util.Map<String, String> blockPlaceholders = new java.util.HashMap<>();
        int placeholderIndex = 0;

        // 首先保护代码块占位符，避免被段落处理破坏
        for (java.util.Map.Entry<String, String> codeEntry : codeBlockPlaceholders.entrySet()) {
            String placeholder = codeEntry.getKey();
            String newPlaceholder = "___BLOCK_PLACEHOLDER_" + placeholderIndex + "___";
            blockPlaceholders.put(newPlaceholder, placeholder);
            placeholderIndex++;
        }

        // 保护已处理的块级元素（标题、列表、引用、代码块等）
        // 这些元素不应该被段落处理影响
        // 注意：不保护行内<code>标签，只保护块级元素，现在包含table（代码块使用table）
        Pattern blockElementPattern = Pattern.compile(
                "(<(?:h[1-6]|ul|ol|li|blockquote|pre|div|table)[^>]*>.*?</(?:h[1-6]|ul|ol|li|blockquote|pre|div|table)>|<(?:h[1-6]|ul|ol|blockquote|pre|div|table)[^>]*/>)",
                Pattern.DOTALL);
        java.util.regex.Matcher blockMatcher = blockElementPattern.matcher(html);
        StringBuffer blockResult = new StringBuffer();
        while (blockMatcher.find()) {
            String blockElement = blockMatcher.group(1);
            String newPlaceholder = "___BLOCK_PLACEHOLDER_" + placeholderIndex + "___";
            blockPlaceholders.put(newPlaceholder, blockElement);
            blockMatcher.appendReplacement(blockResult, newPlaceholder);
            placeholderIndex++;
        }
        blockMatcher.appendTail(blockResult);
        html = blockResult.toString();

        // 先恢复代码块占位符，但暂时用特殊标记保护它们，避免被换行处理影响
        java.util.Map<String, String> tempCodeBlocks = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> entry : blockPlaceholders.entrySet()) {
            String value = entry.getValue();
            // 检查是否是代码块占位符
            if (value.startsWith("___CODE_BLOCK_")) {
                // 恢复实际的代码块HTML，但用临时占位符保护
                String codeBlockHtml = codeBlockPlaceholders.get(value);
                if (codeBlockHtml != null) {
                    String tempPlaceholder = "___TEMP_CODE_BLOCK_" + entry.getKey().substring(entry.getKey().lastIndexOf('_') + 1) + "___";
                    tempCodeBlocks.put(tempPlaceholder, codeBlockHtml);
                    html = html.replace(entry.getKey(), tempPlaceholder);
                }
            } else {
                // 恢复其他块级元素
                html = html.replace(entry.getKey(), value);
            }
        }

        // 现在处理段落分隔和换行（只影响非代码块内容）
        // 先处理双换行（两个或更多连续换行符）转换为段落分隔
        html = html.replaceAll("\n\n+", "</p><p style='margin: 8px 0;'>");
        // 再处理单换行转换为<br/>
        html = html.replaceAll("\n", "<br/>");

        // 最后恢复代码块HTML
        for (java.util.Map.Entry<String, String> entry : tempCodeBlocks.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        // 确保内容被包裹在段落标签中
        String trimmed = html.trim();
        boolean hasParagraphTags = trimmed.contains("</p>") || trimmed.contains("<p style");

        if (hasParagraphTags) {
            // 已经有段落标签，确保开头和结尾正确
            if (!trimmed.startsWith("<p")) {
                html = "<p style='margin: 8px 0;'>" + html;
            }
            if (!trimmed.endsWith("</p>")) {
                html = html + "</p>";
            }
        } else {
            // 没有段落标签，整个包裹
            html = "<p style='margin: 8px 0;'>" + html + "</p>";
        }

        return html;
    }

    /**
     * 获取代码块背景色（根据IDE主题自动适配）
     * 深色主题：使用深灰色背景
     * 浅色主题：使用浅灰色背景
     */
    private static Color getCodeBlockBackground() {
        // 获取IDE面板背景色
        Color panelBg = JBColor.PanelBackground;

        // 判断是否为深色主题（通过检查亮度）
        float[] hsb = Color.RGBtoHSB(panelBg.getRed(), panelBg.getGreen(), panelBg.getBlue(), null);
        boolean isDark = hsb[2] < 0.5f; // 亮度小于0.5认为是深色主题

        if (isDark) {
            // 深色主题：使用稍微深一点的灰色作为代码块背景
            return new Color(45, 45, 45); // #2d2d2d
        } else {
            // 浅色主题：使用浅灰色作为代码块背景
            return new Color(245, 245, 245); // #f5f5f5
        }
    }

    /**
     * 将十六进制颜色字符串变暗
     * 
     * @param hexColor 十六进制颜色字符串（如 #ffffff）
     * @param factor   变暗因子（0.0-1.0，值越大越暗）
     * @return 变暗后的十六进制颜色字符串
     */
    private static String darkenColorHex(String hexColor, double factor) {
        try {
            // 移除 # 号
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

            // 解析RGB值
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            // 变暗
            r = (int) (r * (1 - factor));
            g = (int) (g * (1 - factor));
            b = (int) (b * (1 - factor));

            // 确保值在有效范围内
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            // 转换回十六进制
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            // 如果解析失败，返回原颜色
            return hexColor;
        }
    }

    /**
     * 智能添加代码缩进
     */
    private static String addSmartIndentation(String code) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }

        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        int currentIndent = 0; // 当前缩进级别

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 保留原始缩进，只在需要时添加额外缩进
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                // 空行保持不变
                result.append("\n");
                continue;
            }

            // 先检查是否需要减少缩进（在处理当前行之前）
            if (trimmedLine.startsWith("}") || trimmedLine.startsWith("]")) {
                currentIndent = Math.max(0, currentIndent - 1);
            }

            // 应用当前缩进，保留原始代码的缩进
            String indentStr = "&nbsp;".repeat(currentIndent * 4); // 每级缩进4个空格
            result.append(indentStr).append(line);

            // 再检查是否需要增加缩进（在处理完当前行之后）
            if (trimmedLine.endsWith("{") || trimmedLine.endsWith(":")) {
                currentIndent++;
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }
}
