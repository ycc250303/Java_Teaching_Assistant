package com.javaProgram.ui.components;

import javax.swing.*;

/**
 * 思考提示管理器
 * 负责显示和隐藏"AI正在思考中"的动画提示
 */
public class ThinkingIndicatorManager {
    private final MessageBubbleFactory bubbleFactory;
    private final ChatMessagePanel messagePanel;

    private JPanel thinkingPanel;
    private Timer thinkingTimer;

    public ThinkingIndicatorManager(MessageBubbleFactory bubbleFactory, ChatMessagePanel messagePanel) {
        this.bubbleFactory = bubbleFactory;
        this.messagePanel = messagePanel;
    }

    /**
     * 显示思考提示
     */
    public void show() {
        // 创建思考提示面板
        thinkingPanel = bubbleFactory.createThinkingIndicatorPanel();

        // 获取文本区域引用
        JTextArea thinkingText = (JTextArea) thinkingPanel.getClientProperty("thinkingText");

        if (thinkingText == null) {
            // 兼容旧版本，尝试获取thinkingLabel
            thinkingText = (JTextArea) thinkingPanel.getClientProperty("thinkingLabel");
        }

        if (thinkingText != null) {
            final JTextArea finalThinkingText = thinkingText;

            // 创建动画
            StringBuilder dots = new StringBuilder(".");
            thinkingTimer = new Timer(500, e -> {
                dots.append(".");
                if (dots.length() > 3) {
                    dots.setLength(1);
                }
                finalThinkingText.setText("AI正在思考中" + dots.toString());
            });
            thinkingTimer.start();
        }

        // 添加到消息面板
        messagePanel.addMessage(thinkingPanel, true);
    }

    /**
     * 更新思考提示消息
     * 
     * @param message 新的提示消息
     */
    public void updateMessage(String message) {
        if (thinkingPanel != null) {
            JTextArea thinkingText = (JTextArea) thinkingPanel.getClientProperty("thinkingText");

            if (thinkingText == null) {
                // 兼容旧版本
                thinkingText = (JTextArea) thinkingPanel.getClientProperty("thinkingLabel");
            }

            if (thinkingText != null) {
                final JTextArea finalThinkingText = thinkingText;
                final String baseMessage = message;

                // 停止旧的动画
                if (thinkingTimer != null && thinkingTimer.isRunning()) {
                    thinkingTimer.stop();
                }

                // 创建新的动画
                StringBuilder dots = new StringBuilder(".");
                thinkingTimer = new Timer(500, e -> {
                    dots.append(".");
                    if (dots.length() > 3) {
                        dots.setLength(1);
                    }
                    finalThinkingText.setText(baseMessage + dots.toString());
                });
                thinkingTimer.start();
            }
        }
    }

    /**
     * 隐藏思考提示
     */
    public void hide() {
        if (thinkingTimer != null && thinkingTimer.isRunning()) {
            thinkingTimer.stop();
        }

        if (thinkingPanel != null) {
            messagePanel.removeMessage(thinkingPanel);
            thinkingPanel = null;
            thinkingTimer = null;
        }
    }
}
