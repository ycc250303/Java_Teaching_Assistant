package com.javaProgram.ui.components;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

/**
 * 聊天消息面板
 * 负责管理消息列表的显示、添加和滚动
 */
public class ChatMessagePanel {
    private final JPanel messagesPanel;
    private final JBScrollPane scrollPane;
    private static final int MESSAGE_SPACING = JBUI.scale(2);

    public ChatMessagePanel(Color backgroundColor) {
        // 创建消息容器面板
        messagesPanel = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                Dimension size = super.getMaximumSize();
                return new Dimension(Short.MAX_VALUE, size.height);
            }
        };
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(backgroundColor);
        messagesPanel.setBorder(JBUI.Borders.empty(0));

        // 创建滚动面板
        scrollPane = new JBScrollPane(messagesPanel);
        scrollPane.setBackground(backgroundColor);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(backgroundColor, 0.15f), 1),
                JBUI.Borders.empty(4)));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 添加欢迎消息
        addWelcomeMessage();
    }

    /**
     * 添加欢迎消息
     */
    private void addWelcomeMessage() {
        JLabel welcomeLabel = new JLabel("欢迎使用智能会话助手！");
        welcomeLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, 16f));
        welcomeLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        welcomeLabel.setBorder(JBUI.Borders.empty(20, 8, 20, 50));
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messagesPanel.add(welcomeLabel);
    }

    /**
     * 添加消息到面板
     * 
     * @param messagePanel   消息面板
     * @param scrollToBottom 是否滚动到底部
     */
    public void addMessage(JPanel messagePanel, boolean scrollToBottom) {
        // 移除欢迎文本
        if (messagesPanel.getComponentCount() > 0) {
            Component firstComponent = messagesPanel.getComponent(0);
            if (firstComponent instanceof JLabel &&
                    ((JLabel) firstComponent).getText().contains("欢迎使用")) {
                messagesPanel.removeAll();
            }
        }

        // 添加间距（除了第一条消息）
        if (messagesPanel.getComponentCount() > 0) {
            JPanel spacer = createSpacer();
            messagesPanel.add(spacer);
        }

        // 添加消息
        messagesPanel.add(messagePanel);
        messagesPanel.revalidate();
        messagesPanel.repaint();

        // 滚动到底部
        if (scrollToBottom) {
            SwingUtilities.invokeLater(() -> {
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                scrollBar.setValue(scrollBar.getMaximum());
            });
        }
    }

    /**
     * 移除消息
     * 
     * @param messagePanel 要移除的消息面板
     */
    public void removeMessage(JPanel messagePanel) {
        messagesPanel.remove(messagePanel);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    /**
     * 创建间距面板
     */
    private JPanel createSpacer() {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(0, MESSAGE_SPACING));
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, MESSAGE_SPACING));
        spacer.setMinimumSize(new Dimension(0, MESSAGE_SPACING));
        spacer.setLayout(new BorderLayout());
        spacer.add(new JLabel(), BorderLayout.CENTER);
        return spacer;
    }

    /**
     * 使颜色变浅
     */
    private Color lightenColor(Color color, float factor) {
        int red = (int) Math.min(255, color.getRed() + (255 - color.getRed()) * factor);
        int green = (int) Math.min(255, color.getGreen() + (255 - color.getGreen()) * factor);
        int blue = (int) Math.min(255, color.getBlue() + (255 - color.getBlue()) * factor);
        return new Color(red, green, blue, color.getAlpha());
    }

    /**
     * 获取滚动面板（用于添加到父容器）
     */
    public JBScrollPane getScrollPane() {
        return scrollPane;
    }
}
