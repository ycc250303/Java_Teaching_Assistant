package com.javaProgram.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * 消息气泡工厂类
 * 负责创建用户消息和AI消息的UI组件
 */
public class MessageBubbleFactory {
    private static final float DEFAULT_FONT_SIZE = 14f;
    private static final float SMALL_FONT_SIZE = 13f;
    private static final float MINI_FONT_SIZE = 12f;

    private int userPreferredHeight;
    private JBScrollPane chatScrollPane;

    public MessageBubbleFactory(JBScrollPane chatScrollPane) {
        this.chatScrollPane = chatScrollPane;
    }

    /**
     * 创建用户消息气泡（右侧带框，自适应大小）
     */
    public JPanel createUserMessageBubble(String message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);

        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // 创建左侧占位面板
        JPanel leftSpacer = new JPanel();
        leftSpacer.setOpaque(false);
        leftSpacer.setPreferredSize(new Dimension(JBUI.scale(50), 1));

        // 创建右侧消息容器
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        // 创建内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 用户标签
        JLabel userLabel = new JLabel("励志学习java的小学生 🎓");
        userLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.BOLD));
        userLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        userLabel.setBorder(JBUI.Borders.empty(1, 12, 1, 0));
        userLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(userLabel);

        // 消息文本
        JTextArea messageText = createAutoSizingTextArea(message);
        messageText.setOpaque(true);
        messageText.setBackground(lightenColor(JBColor.PanelBackground, 0.05f));
        messageText.setForeground(JBUI.CurrentTheme.Label.foreground());
        messageText.setFont(JBUI.Fonts.smallFont());
        messageText.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(JBColor.PanelBackground, 0.2f), 1),
                JBUI.Borders.empty(2, 4)));
        messageText.setFocusable(false);
        messageText.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        contentPanel.add(messageText);

        // 时间标签
        JLabel timeLabel = new JLabel(
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setFont(JBUI.Fonts.miniFont());
        timeLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        timeLabel.setBorder(JBUI.Borders.empty(4, 4, 2, 0));
        timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(timeLabel);

        rightPanel.add(contentPanel);

        messagePanel.setPreferredSize(new Dimension(Short.MAX_VALUE, userPreferredHeight + 80));
        messagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, userPreferredHeight + 80));

        messagePanel.add(leftSpacer, BorderLayout.WEST);
        messagePanel.add(rightPanel, BorderLayout.CENTER);
        messagePanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(JBColor.PanelBackground, 0.2f), 1),
                JBUI.Borders.empty(4, 8)));
        return messagePanel;
    }

    /**
     * 创建AI消息气泡
     */
    public JPanel createAiMessageBubble(String message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);

        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // AI标签
        JLabel aiLabel = new JLabel("AI小老师 👨‍🏫");
        aiLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE));
        aiLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        aiLabel.setBorder(JBUI.Borders.empty(1, 8, 1, 8));

        // AI消息文本
        JTextArea messageText = new JTextArea(message);
        messageText.setEditable(false);
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setOpaque(false);
        messageText.setForeground(JBUI.CurrentTheme.Label.foreground());
        messageText.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        messageText.setBorder(JBUI.Borders.empty(0, 8, 2, 8));
        messageText.setFocusable(false);

        int viewportWidth = chatScrollPane != null ? chatScrollPane.getViewport().getWidth() : 400;
        int maxWidth = Math.max(200, viewportWidth - 60);
        messageText.setSize(new Dimension(maxWidth, 1));

        messagePanel.add(aiLabel, BorderLayout.NORTH);
        messagePanel.add(messageText, BorderLayout.CENTER);
        messagePanel.setBorder(JBUI.Borders.empty(0, 8, 0, 8));
        return messagePanel;
    }

    /**
     * 创建自适应大小的文本区域
     */
    private JTextArea createAutoSizingTextArea(String text) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(text);
        textArea.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));

        FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = metrics.getHeight();

        int maxTextWidth = JBUI.scale(250);
        int minTextWidth = JBUI.scale(20);

        String[] lines = text.split("\n");
        int maxLineLength = 0;
        int totalLines = 0;

        for (String line : lines) {
            int lineWidth = metrics.stringWidth(line);
            if (lineWidth > maxLineLength) {
                maxLineLength = lineWidth;
            }
            totalLines++;
        }

        if (maxLineLength > maxTextWidth) {
            int estimatedLines = 0;
            for (String line : lines) {
                int estimatedLineLength = (int) Math.ceil((double) metrics.stringWidth(line) / maxTextWidth);
                estimatedLines += Math.max(1, estimatedLineLength);
            }
            totalLines = estimatedLines;
            maxLineLength = maxTextWidth;
        }

        int insetsWidth = textArea.getInsets().left + textArea.getInsets().right + JBUI.scale(24);
        int insetsHeight = textArea.getInsets().top + textArea.getInsets().bottom + JBUI.scale(16);

        int preferredWidth = Math.max(minTextWidth, Math.min(maxLineLength + insetsWidth, maxTextWidth + insetsWidth));
        int preferredHeight = Math.max(1, totalLines) * lineHeight + insetsHeight;

        userPreferredHeight = preferredHeight;

        textArea.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        textArea.setMaximumSize(new Dimension(preferredWidth, preferredHeight));

        return textArea;
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
}
