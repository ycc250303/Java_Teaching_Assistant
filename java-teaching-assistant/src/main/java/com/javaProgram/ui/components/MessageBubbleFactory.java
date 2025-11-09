package com.javaProgram.ui.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.javaProgram.services.ContextService;
import com.javaProgram.utils.CodeNavigationUtil;
import com.javaProgram.utils.MarkdownToHtml;

import javax.swing.*;
import javax.swing.border.AbstractBorder;

import java.awt.*;
import java.util.List;

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
    private Project project;

    /**
     * 圆角边框 - 内部类实现
     */
    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;
        private final int padding;

        public RoundedBorder(Color color, int thickness, int radius, int padding) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
            this.padding = padding;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);

            // 绘制圆角矩形边框
            int offset = thickness / 2;
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawRoundRect(
                    x + offset,
                    y + offset,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius);

            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int value = padding + thickness;
            return new Insets(value, value, value, value);
        }
    }

    public MessageBubbleFactory(JBScrollPane chatScrollPane, Project project) {
        this.chatScrollPane = chatScrollPane;
        this.project = project;
    }

    /**
     * 创建用户消息气泡（右侧带框，自适应大小）
     */
    public JPanel createUserMessageBubble(String message) {
        return createUserMessageBubble(message, null);
    }

    /**
     * 创建用户消息气泡（带上下文信息）
     * 
     * @param message      用户消息文本
     * @param contextItems 上下文列表（可为null）
     */
    public JPanel createUserMessageBubble(String message, List<ContextService.ContextItem> contextItems) {
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

        // 创建内容面板（使用 BorderLayout 以固定时间位置）
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        // 创建消息内容容器（包含消息文本和上下文标签）
        JPanel messageContentPanel = new JPanel();
        messageContentPanel.setLayout(new BoxLayout(messageContentPanel, BoxLayout.Y_AXIS));
        messageContentPanel.setOpaque(false);

        // 消息文本
        JTextArea messageText = createAutoSizingTextArea(message);
        messageText.setOpaque(true);
        messageText.setBackground(lightenColor(JBColor.PanelBackground, 0.05f));
        messageText.setForeground(JBUI.CurrentTheme.Label.foreground());
        messageText.setFont(JBUI.Fonts.smallFont());

        // 使用圆角边框
        messageText.setBorder(new RoundedBorder(
                lightenColor(JBColor.PanelBackground, 0.2f),
                1,
                JBUI.scale(8),
                JBUI.scale(6)));

        messageText.setFocusable(true);
        messageText.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        messageContentPanel.add(messageText);

        // 如果有上下文，添加上下文标签
        if (contextItems != null && !contextItems.isEmpty()) {
            JPanel contextTagsPanel = createContextTagsPanel(contextItems);
            messageContentPanel.add(Box.createVerticalStrut(JBUI.scale(4)));
            messageContentPanel.add(contextTagsPanel);
        }

        contentPanel.add(messageContentPanel, BorderLayout.CENTER);

        // 时间标签容器（固定在右下角）
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        timePanel.setOpaque(false);

        JLabel timeLabel = new JLabel(
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setFont(JBUI.Fonts.miniFont());
        timeLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        timeLabel.setBorder(JBUI.Borders.empty(4, 4, 2, 4));

        timePanel.add(timeLabel);
        contentPanel.add(timePanel, BorderLayout.SOUTH);

        rightPanel.add(contentPanel);

        messagePanel.setPreferredSize(new Dimension(Short.MAX_VALUE, userPreferredHeight + 80));
        messagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, userPreferredHeight + 80));

        messagePanel.add(leftSpacer, BorderLayout.WEST);
        messagePanel.add(rightPanel, BorderLayout.CENTER);
        messagePanel.setBorder(JBUI.Borders.empty(2, 8));

        return messagePanel;
    }

    /**
     * 创建上下文标签面板
     */
    private JPanel createContextTagsPanel(List<ContextService.ContextItem> contextItems) {
        JPanel tagsPanel = new JPanel();
        tagsPanel.setLayout(new BoxLayout(tagsPanel, BoxLayout.Y_AXIS));
        tagsPanel.setOpaque(false);
        tagsPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        for (ContextService.ContextItem item : contextItems) {
            JPanel tagPanel = createContextTag(item);
            tagsPanel.add(tagPanel);
            if (contextItems.indexOf(item) < contextItems.size() - 1) {
                tagsPanel.add(Box.createVerticalStrut(JBUI.scale(2)));
            }
        }

        return tagsPanel;
    }

    /**
     * 创建单个上下文标签
     */
    private JPanel createContextTag(ContextService.ContextItem item) {
        JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(4), 0));
        tagPanel.setOpaque(false);

        // 📎 图标
        JLabel iconLabel = new JLabel("📎");
        iconLabel.setFont(JBUI.Fonts.miniFont());
        tagPanel.add(iconLabel);

        // 文件名和行号信息
        StringBuilder tagText = new StringBuilder();
        tagText.append(item.getFileName());

        if (item.getStartLine() > 0 && item.getEndLine() > 0) {
            tagText.append(" (").append(item.getStartLine())
                    .append("-").append(item.getEndLine()).append("行)");
        }

        JLabel textLabel = new JLabel(tagText.toString());
        textLabel.setFont(JBUI.Fonts.miniFont());

        // 设置默认颜色
        Color defaultColor = new JBColor(new Color(102, 102, 102), new Color(153, 153, 153));
        Color hoverColor = new JBColor(new Color(0, 120, 215), new Color(100, 149, 237));
        textLabel.setForeground(defaultColor);

        // 添加可点击效果（悬停变色 + 手型光标）
        CodeNavigationUtil.addClickableEffect(textLabel, defaultColor, hoverColor);

        // 添加点击事件 - 跳转到代码位置
        textLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                CodeNavigationUtil.navigateToCode(project, item, tagPanel);
            }
        });

        tagPanel.add(textLabel);

        return tagPanel;
    }

    /**
     * 创建AI消息的文本区域（内部方法，可复用）
     * 使用 JEditorPane 支持 HTML 渲染
     * 
     * @return 配置好样式的 JEditorPane
     */
    private JEditorPane createAiTextArea() {
        JEditorPane messageText = new JEditorPane();
        messageText.setContentType("text/html");
        messageText.setEditable(false);
        messageText.setOpaque(false);
        messageText.setBorder(JBUI.Borders.empty(0, 8, 2, 8));
        messageText.setFocusable(true);
        
        // 设置背景透明，使用主题颜色
        messageText.setBackground(new Color(0, 0, 0, 0));
        
        return messageText;
    }

    /**
     * 创建AI消息面板（内部方法，可复用）
     * 
     * @param messageText AI消息的文本区域（JEditorPane）
     * @return 包装好的消息面板
     */
    private JPanel createAiMessagePanel(JEditorPane messageText) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        messagePanel.add(messageText, BorderLayout.CENTER);
        messagePanel.setBorder(JBUI.Borders.empty(2, 8, 2, 8));
        return messagePanel;
    }

    /**
     * 创建AI消息气泡（用于显示完整的AI消息）
     * 
     * @param message AI回复的完整消息内容（Markdown格式）
     * @return 包含消息的面板
     */
    public JPanel createAiMessageBubble(String message) {
        JEditorPane messageText = createAiTextArea();
        
        // 获取IDE主题的文本颜色
        Color textColor = JBColor.foreground();
        
        // 将Markdown转换为HTML，使用主题颜色
        String html = MarkdownToHtml.convert(message, textColor);
        messageText.setText(html);

        // 根据滚动面板宽度设置文本区域大小
        int viewportWidth = chatScrollPane != null ? chatScrollPane.getViewport().getWidth() : 400;
        int maxWidth = Math.max(200, viewportWidth - 60);
        messageText.setSize(new Dimension(maxWidth, 1));

        return createAiMessagePanel(messageText);
    }

    /**
     * 创建流式AI消息的文本区域（用于逐步接收AI回复）
     * 
     * @return 空的、配置好样式的 JEditorPane，可用于逐步追加内容
     */
    public JEditorPane createStreamingAiTextArea() {
        return createAiTextArea();
    }

    /**
     * 创建流式AI消息面板（用于包装流式接收的AI消息）
     * 
     * @param messageText 已创建的文本区域（JEditorPane）
     * @return 配置好的消息面板，支持动态调整大小
     */
    public JPanel createStreamingAiMessagePanel(JEditorPane messageText) {
        JPanel messagePanel = createAiMessagePanel(messageText);

        // 流式消息需要支持动态调整大小
        messagePanel.setPreferredSize(new Dimension(Short.MAX_VALUE, 50));
        messagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Integer.MAX_VALUE));
        messagePanel.setMinimumSize(new Dimension(200, 50));

        return messagePanel;
    }

    /**
     * 创建思考中提示面板
     * 
     * @return 配置好样式的思考提示面板
     */
    public JPanel createThinkingIndicatorPanel() {
        JPanel thinkingPanel = new JPanel(new BorderLayout());
        thinkingPanel.setOpaque(false);
        thinkingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkingPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // 创建思考标签
        JTextArea thinkingLabel = new JTextArea("AI正在思考中");
        thinkingLabel.setEditable(false);
        thinkingLabel.setLineWrap(true);
        thinkingLabel.setWrapStyleWord(true);
        thinkingLabel.setOpaque(false);
        thinkingLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        thinkingLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        thinkingLabel.setBorder(JBUI.Borders.empty(0, 8, 2, 8));
        thinkingLabel.setFocusable(true);

        thinkingPanel.add(thinkingLabel, BorderLayout.CENTER);
        thinkingPanel.setBorder(JBUI.Borders.empty(2, 8, 2, 8));

        // 将标签保存为客户端属性，方便外部更新文本
        thinkingPanel.putClientProperty("thinkingLabel", thinkingLabel);

        return thinkingPanel;
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
