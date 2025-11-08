package com.javaProgram.ui.handlers;

import com.intellij.ui.JBColor;
import com.javaProgram.ui.components.MessageBubbleFactory;
import com.javaProgram.ui.components.ChatMessagePanel;
import javax.swing.*;
import java.awt.*;

/**
 * AI响应处理器
 * 负责处理AI流式响应的显示和更新
 */
public class AiResponseHandler {
    private final MessageBubbleFactory bubbleFactory;
    private final ChatMessagePanel messagePanel;

    private JTextArea currentAiMessage;
    private JPanel currentAiMessagePanel;

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

        currentAiMessagePanel = bubbleFactory.createStreamingAiMessagePanel(currentAiMessage);

        messagePanel.addMessage(currentAiMessagePanel, true);
    }

    /**
     * 追加响应内容
     */
    public void appendChunk(String chunk) {
        if (currentAiMessage != null) {
            SwingUtilities.invokeLater(() -> {
                currentAiMessage.append(chunk);

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
    }

    /**
     * 添加错误消息
     */
    public void addError(String error) {
        if (currentAiMessage != null) {
            SwingUtilities.invokeLater(() -> {
                currentAiMessage.append("\n[错误] " + error + "\n");
                currentAiMessage.setForeground(JBColor.RED);
            });
        } else {
            JPanel errorPanel = bubbleFactory.createAiMessageBubble("[错误] " + error);
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

            // 计算文本区域需要的实际高度
            int textHeight = currentAiMessage.getPreferredSize().height;
            int totalHeight = Math.max(50, textHeight + 50);

            currentAiMessagePanel.setPreferredSize(new Dimension(maxWidth, totalHeight));
            currentAiMessagePanel.setMaximumSize(new Dimension(maxWidth, totalHeight));
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
