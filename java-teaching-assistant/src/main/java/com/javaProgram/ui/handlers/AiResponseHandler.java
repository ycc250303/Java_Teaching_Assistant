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
        currentAiMessage.setSize(new Dimension(Short.MAX_VALUE - 100, 1));
        accumulatedMarkdown = new StringBuilder(); // 初始化累积内容

        currentAiMessagePanel = bubbleFactory.createStreamingAiMessagePanel(currentAiMessage);

        messagePanel.addMessage(currentAiMessagePanel, true);
    }

    /**
     * 追加响应内容
     */
    public void appendChunk(String chunk) {
        if (currentAiMessage != null && accumulatedMarkdown != null) {
            SwingUtilities.invokeLater(() -> {
                // 累积Markdown内容
                accumulatedMarkdown.append(chunk);
                
                // 获取IDE主题的文本颜色
                Color textColor = JBColor.foreground();
                
                // 将累积的Markdown转换为HTML并更新显示
                String html = MarkdownToHtml.convert(accumulatedMarkdown.toString(), textColor);
                currentAiMessage.setText(html);

                // 强制文本区域重新计算大小和换行
                currentAiMessage.revalidate();
                currentAiMessage.repaint();

                // 更新面板大小
                updatePanelSize();

                // 滚动到底部
                scrollToBottom();
            });
        }
    }

    /**
     * 完成响应
     */
    public void finishResponse() {
        currentAiMessage = null;
        currentAiMessagePanel = null;
        accumulatedMarkdown = null;
    }

    /**
     * 添加错误消息
     */
    public void addError(String error) {
        if (currentAiMessage != null && accumulatedMarkdown != null) {
            SwingUtilities.invokeLater(() -> {
                // 添加错误信息到累积内容
                accumulatedMarkdown.append("\n\n**错误**: ").append(error);
                // 获取IDE主题的文本颜色（错误消息使用红色）
                Color errorColor = new Color(211, 47, 47); // #d32f2f
                String html = MarkdownToHtml.convert(accumulatedMarkdown.toString(), errorColor);
                currentAiMessage.setText(html);
            });
        } else {
            JPanel errorPanel = bubbleFactory.createAiMessageBubble("**错误**: " + error);
            messagePanel.addMessage(errorPanel, true);
        }
        finishResponse();
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
