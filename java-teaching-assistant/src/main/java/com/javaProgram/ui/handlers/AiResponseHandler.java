package com.javaProgram.ui.handlers;

import com.intellij.ui.JBColor;
import com.javaProgram.ui.components.MessageBubbleFactory;
import com.javaProgram.ui.components.ChatMessagePanel;
import com.javaProgram.utils.MarkdownToHtml;
import javax.swing.*;
import java.awt.*;

/**
 * AI响应处理器
 * 负责处理AI流式响应的显示和更新
 */
public class AiResponseHandler {
    private final MessageBubbleFactory bubbleFactory;
    private final ChatMessagePanel messagePanel;

    private JEditorPane currentAiMessage;
    private JPanel currentAiMessagePanel;
    private StringBuilder accumulatedMarkdown; // 累积的Markdown内容，用于流式更新

    // 更新节流相关
    private volatile long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 100; // 每100ms最多更新一次
    private Timer updateTimer; // 定时器，用于确保最终更新

    public AiResponseHandler(MessageBubbleFactory bubbleFactory, ChatMessagePanel messagePanel) {
        this.bubbleFactory = bubbleFactory;
        this.messagePanel = messagePanel;
    }

    /**
     * 判断当前是否空闲（没有正在处理的响应）
     */
    public boolean isIdle() {
        return currentAiMessage == null;
    }

    /**
     * 开始AI响应
     */
    public void startResponse() {
        currentAiMessage = bubbleFactory.createStreamingAiTextArea();
        // 设置合理的宽度约束，确保文本能够正常换行
        // 获取聊天面板的宽度作为参考
        int chatPanelWidth = messagePanel.getScrollPane().getWidth();
        int maxWidth = chatPanelWidth > 0 ? chatPanelWidth - 100 : 600; // 减去边距
        currentAiMessage.setSize(new Dimension(maxWidth, 1));
        accumulatedMarkdown = new StringBuilder(); // 初始化累积内容

        currentAiMessagePanel = bubbleFactory.createStreamingAiMessagePanel(currentAiMessage);

        messagePanel.addMessage(currentAiMessagePanel, true);
    }

    /**
     * 追加响应内容
     */
    public void appendChunk(String chunk) {
        if (currentAiMessage != null && accumulatedMarkdown != null) {
            // 累积Markdown内容
            // 智能添加换行符：如果是代码行且不以换行符结尾，则添加换行符
            if (shouldAddNewline(chunk, accumulatedMarkdown.toString())) {
                accumulatedMarkdown.append(chunk).append("\n");
            } else {
                accumulatedMarkdown.append(chunk);
            }

            // 更新节流：检查是否需要立即更新
            long currentTime = System.currentTimeMillis();
            boolean shouldUpdate = (currentTime - lastUpdateTime) >= UPDATE_INTERVAL_MS;

            if (shouldUpdate) {
                lastUpdateTime = currentTime;
                updateDisplaySafely();
            } else {
                // 如果不需要立即更新，则使用定时器延迟更新（确保最终状态能显示）
                scheduleDelayedUpdate();
            }
        }
    }

    /**
     * 判断是否需要在chunk后添加换行符
     *
     * @param chunk 当前chunk
     * @param previousContent 之前累积的内容
     * @return 是否需要添加换行符
     */
    private boolean shouldAddNewline(String chunk, String previousContent) {
        // 如果chunk为空或已经是换行符，不需要添加
        if (chunk == null || chunk.isEmpty() || chunk.endsWith("\n")) {
            return false;
        }

        // 检查是否在代码块中
        boolean inCodeBlock = isInCodeBlock(previousContent);

        // 如果是代码块开始标记，不添加换行符
        if (chunk.trim().startsWith("```")) {
            return false;
        }

        // 如果在代码块中，使用更智能的换行逻辑
        if (inCodeBlock && !chunk.trim().equals("```")) {
            // 检查chunk是否是代码行的开始
            String trimmed = chunk.trim();

            // 如果是代码块结束标记，不添加换行符
            if (trimmed.equals("```")) {
                return false;
            }

            // 检查是否需要换行的几种情况：
            // 1. 明确的语句结束符
            if (trimmed.endsWith(";") || trimmed.endsWith("{") || trimmed.endsWith("}")) {
                return true;
            }

            // 2. 方法声明结束后（有括号但没有分号）
            if (trimmed.matches(".*\\)\\s*$") && !trimmed.contains("(") || trimmed.matches(".*\\)\\s*throws\\s+\\w+")) {
                return true;
            }

            // 3. 注释行
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                return true;
            }

            // 4. 访问修饰符行（如public, private, static等单独出现）
            if (trimmed.equals("public") || trimmed.equals("private") || trimmed.equals("protected") ||
                trimmed.equals("static") || trimmed.equals("final") || trimmed.equals("abstract")) {
                return false; // 这些通常是多行声明的一部分，暂时不换行
            }

            // 5. 空行或者只有空格的行
            if (trimmed.isEmpty()) {
                return true;
            }

            return false;
        }

        // 非代码块内容，不添加换行符
        return false;
    }

    /**
     * 判断是否像代码语句
     */
    private boolean looksLikeCodeStatement(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return false;
        }

        String trimmed = chunk.trim();

        // 检查常见的代码模式
        return trimmed.contains("{") || trimmed.contains("}") ||
               trimmed.contains(";") || trimmed.contains("(") ||
               trimmed.contains("class ") || trimmed.contains("public ") ||
               trimmed.contains("private ") || trimmed.contains("static ") ||
               trimmed.contains("void ") || trimmed.contains("int ") ||
               trimmed.contains("String ") || trimmed.contains("System.out") ||
               trimmed.matches(".*\\s+\\w+\\s*\\(.*"); // 方法调用模式
    }

    /**
     * 检查是否在代码块中
     */
    private boolean isInCodeBlock(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 计算未闭合的代码块标记数量
        int codeBlockCount = 0;
        int index = 0;

        while (index < content.length()) {
            int found = content.indexOf("```", index);
            if (found == -1) {
                break;
            }
            codeBlockCount++;
            index = found + 3;
        }

        // 如果有奇数个```，说明在代码块中
        return codeBlockCount % 2 == 1;
    }

    
    /**
     * 安全地更新显示内容（带异常保护）
     */
    private void updateDisplaySafely() {
        // 在外部捕获变量的引用，避免定时器回调时访问到null
        final JEditorPane messageRef = currentAiMessage;
        final StringBuilder markdownRef = accumulatedMarkdown;

        if (messageRef == null || markdownRef == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                String markdownText = markdownRef.toString();

                // 获取IDE主题的文本颜色
                Color textColor = JBColor.foreground();

                // 将累积的Markdown转换为HTML
                String html = MarkdownToHtml.convert(markdownText, textColor);

                // 安全地设置HTML内容
                if (html != null && !html.isEmpty()) {
                    // 使用 read() 方法替代 setText()，可以更好地处理HTML解析错误
                    // 先清空内容，避免累积错误
                    messageRef.setText("");

                    // 使用 StringReader 和 read() 方法，这样可以更好地处理解析错误
                    try (java.io.StringReader reader = new java.io.StringReader(html)) {
                        messageRef.read(reader, null);
                    }

                    // 强制文本区域重新计算大小和换行
                    messageRef.revalidate();
                    messageRef.repaint();

                    // 更新面板大小
                    updatePanelSize();

                    // 滚动到底部
                    scrollToBottom();
                }
            } catch (Exception e) {
                // 捕获所有异常，避免界面卡死
                System.err.println("Error updating AI response display: " + e.getMessage());
                e.printStackTrace();

                // 如果出现异常，尝试使用纯文本显示
                try {
                    if (messageRef != null && markdownRef != null) {
                        messageRef.setContentType("text/plain");
                        messageRef.setText(markdownRef.toString());
                    }
                } catch (Exception fallbackEx) {
                    System.err.println("Fallback text display also failed: " + fallbackEx.getMessage());
                }
            }
        });
    }

    /**
     * 调度延迟更新（用于确保最终状态能显示）
     */
    private void scheduleDelayedUpdate() {
        if (updateTimer != null && updateTimer.isRunning()) {
            updateTimer.stop();
        }

        updateTimer = new Timer((int) UPDATE_INTERVAL_MS, e -> {
            updateDisplaySafely();
            lastUpdateTime = System.currentTimeMillis();
        });
        updateTimer.setRepeats(false);
        updateTimer.start();
    }

    /**
     * 完成响应
     */
    public void finishResponse() {
        // 先停止定时器，避免竞态条件
        if (updateTimer != null) {
            if (updateTimer.isRunning()) {
                updateTimer.stop();
            }
            updateTimer = null;
        }

        // 确保最终状态被显示（在清理资源之前）
        if (currentAiMessage != null && accumulatedMarkdown != null) {
            updateDisplaySafely();
        }

        // 清理资源
        currentAiMessage = null;
        currentAiMessagePanel = null;
        accumulatedMarkdown = null;
        lastUpdateTime = 0;
    }

    /**
     * 添加错误消息
     */
    public void addError(String error) {
        // 先停止定时器
        if (updateTimer != null) {
            if (updateTimer.isRunning()) {
                updateTimer.stop();
            }
            updateTimer = null;
        }

        if (currentAiMessage != null && accumulatedMarkdown != null) {
            // 添加错误信息到累积内容
            accumulatedMarkdown.append("\n\n**错误**: ").append(error);

            // 捕获引用，避免竞态条件
            final JEditorPane messageRef = currentAiMessage;
            final StringBuilder markdownRef = accumulatedMarkdown;

            // 安全地更新显示
            SwingUtilities.invokeLater(() -> {
                try {
                    // 获取IDE主题的文本颜色（错误消息使用红色）
                    Color errorColor = new Color(211, 47, 47); // #d32f2f
                    String html = MarkdownToHtml.convert(markdownRef.toString(), errorColor);
                    if (html != null && !html.isEmpty() && messageRef != null) {
                        // 使用 read() 方法替代 setText()
                        messageRef.setText("");
                        try (java.io.StringReader reader = new java.io.StringReader(html)) {
                            messageRef.read(reader, null);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error displaying error message: " + e.getMessage());
                    // 如果HTML显示失败，尝试纯文本
                    try {
                        if (messageRef != null && markdownRef != null) {
                            messageRef.setContentType("text/plain");
                            messageRef.setText(markdownRef.toString());
                        }
                    } catch (Exception fallbackEx) {
                        System.err.println("Fallback error display also failed: " + fallbackEx.getMessage());
                    }
                }
            });
        } else {
            JPanel errorPanel = bubbleFactory.createAiMessageBubble("**错误**: " + error);
            messagePanel.addMessage(errorPanel, true);
        }

        // 清理资源
        currentAiMessage = null;
        currentAiMessagePanel = null;
        accumulatedMarkdown = null;
        lastUpdateTime = 0;
    }

    /**
     * 更新面板大小以适应内容
     */
    private void updatePanelSize() {
        if (currentAiMessagePanel != null && currentAiMessage != null) {
            // 保持之前设置的宽度限制
            int maxWidth = currentAiMessagePanel.getPreferredSize().width;

            // 对于JEditorPane，使用getPreferredSize()获取内容高度
            // 如果内容高度不可用，则使用固定值或根据内容估算
            Dimension preferredSize = currentAiMessage.getPreferredSize();
            int textHeight = preferredSize.height > 0 ? preferredSize.height : 100;
            int totalHeight = Math.max(50, textHeight + 50);

            currentAiMessagePanel.setPreferredSize(new Dimension(maxWidth, totalHeight));
            currentAiMessagePanel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
            currentAiMessagePanel.setMinimumSize(new Dimension(200, totalHeight));

            currentAiMessagePanel.revalidate();
            currentAiMessagePanel.repaint();
        }
    }

    /**
     * 滚动到底部
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = messagePanel.getScrollPane();
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            scrollBar.setValue(scrollBar.getMaximum());
        });
    }
}
