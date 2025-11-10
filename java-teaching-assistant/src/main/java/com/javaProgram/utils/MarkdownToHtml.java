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
        Pattern codeBlockPattern = Pattern.compile("```(\\w*)?\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
        java.util.regex.Matcher codeBlockMatcher = codeBlockPattern.matcher(html);
        StringBuffer codeBlockResult = new StringBuffer();
        while (codeBlockMatcher.find()) {
            // String language = codeBlockMatcher.group(1); // 暂时不使用语言信息
            String code = codeBlockMatcher.group(2);
            // 转义代码块内的HTML特殊字符
            code = escapeHtml(code);
            String placeholder = "___CODE_BLOCK_" + placeholderIndex + "___";
            // 使用div替代pre，更好地控制换行（移除不兼容的CSS属性）
            // 移除 white-space: pre-wrap，使用更兼容的方式
            codeBlockPlaceholders.put(placeholder,
                    "<div style='background-color: " + codeBlockBgHex + "; border: 1px solid "
                            + darkenColorHex(codeBlockBgHex, 0.2)
                            + "; padding: 10px; margin: 8px 0; font-family: monospace; font-size: 12px; color: "
                            + codeTextColorHex + ";'>" + code
                            + "</div>");
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
        html = processParagraphs(html);

        // 13. 包裹在div中，设置基本样式（使用简单的字体族，避免引号问题）
        // 获取文本颜色（RGB十六进制）
        String colorHex = textColor != null ? colorToHex(textColor) : "#333333";
        html = "<div style='font-family: Arial, sans-serif; font-size: 14px; line-height: 1.6; color: " + colorHex
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
        // 简单的实现：找到连续的<li>标签并包裹
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
    private static String processParagraphs(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }

        // 使用占位符保护块级元素，避免段落处理影响它们
        java.util.Map<String, String> blockPlaceholders = new java.util.HashMap<>();
        int placeholderIndex = 0;

        // 保护已处理的块级元素（标题、列表、引用、代码块等）
        // 这些元素不应该被段落处理影响
        // 注意：不保护行内<code>标签，只保护块级元素
        Pattern blockElementPattern = Pattern.compile(
                "(<(?:h[1-6]|ul|ol|li|blockquote|pre)[^>]*>.*?</(?:h[1-6]|ul|ol|li|blockquote|pre)>|<(?:h[1-6]|ul|ol|blockquote|pre)[^>]*/>)",
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

        // 现在处理段落分隔和换行
        // 先处理双换行（两个或更多连续换行符）转换为段落分隔
        html = html.replaceAll("\n\n+", "</p><p style='margin: 8px 0;'>");
        // 再处理单换行转换为<br/>
        html = html.replaceAll("\n", "<br/>");

        // 恢复块级元素占位符
        for (java.util.Map.Entry<String, String> entry : blockPlaceholders.entrySet()) {
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
}
