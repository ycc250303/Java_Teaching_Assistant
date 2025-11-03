package com.javaProgram.ui;

import com.javaProgram.services.AiServiceClient;
import com.javaProgram.services.ContextService;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.*;

public class ChatToolWindowContent {
    private static final float DEFAULT_FONT_SIZE = 14f; // 默认字体大小
    private static final float SMALL_FONT_SIZE = 13f; // 小字体大小
    private static final float MINI_FONT_SIZE = 12f; // 最小字体大小

    private final JPanel mainPanel;
    private final Project project;
    private final AiServiceClient aiClient;
    private final ContextService contextService;

    // 上下文状态显示
    private JLabel contextStatusLabel;

    private static final int MESSAGE_SPACING = JBUI.scale(4); // 消息之间的垂直间距（4像素）

    // 聊天相关组件
    private final JPanel chatMessagesPanel;
    private JBScrollPane chatScrollPane;

    // 当前正在构建的AI回复
    private JTextArea currentAiMessage;
    private JPanel currentAiMessagePanel;

    // 思考中提示组件
    private JPanel thinkingPanel;
    private JLabel thinkingLabel;
    private Timer thinkingTimer;

    // 添加一个辅助方法来使颜色变浅
    private Color lightenColor(Color color, float factor) {
        int red = (int) Math.min(255, color.getRed() + (255 - color.getRed()) * factor);
        int green = (int) Math.min(255, color.getGreen() + (255 - color.getGreen()) * factor);
        int blue = (int) Math.min(255, color.getBlue() + (255 - color.getBlue()) * factor);
        return new Color(red, green, blue, color.getAlpha());
    }

    // 创建用户消息气泡（右侧带框，自适应大小）
    private JPanel createUserMessageBubble(String message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);

        // 关键修复：在BoxLayout中设置正确的对齐方式
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // 创建左侧占位面板，给用户消息留出左边距
        JPanel leftSpacer = new JPanel();
        leftSpacer.setOpaque(false);
        leftSpacer.setPreferredSize(new Dimension(JBUI.scale(50), 1)); // 固定左边距50像素

        // 创建右侧消息容器
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        // 创建内容面板，用于垂直排列组件
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 将用户标签添加到内容面板
        JLabel userLabel = new JLabel("励志学习java的小学生 🎓");
        userLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.BOLD));
        userLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        userLabel.setBorder(JBUI.Borders.empty(1, 12, 1, 0));
        userLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(userLabel);

        // 使用原有的尺寸计算方法创建自适应大小的文本区域
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

        // 添加时间标签
        JLabel timeLabel = new JLabel(
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.setFont(JBUI.Fonts.miniFont());
        timeLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        timeLabel.setBorder(JBUI.Borders.empty(4, 4, 2, 0));
        timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); // 右对齐
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentPanel.add(timeLabel);

        // 将内容面板添加到rightPanel
        rightPanel.add(contentPanel);

        // 根据计算的高度设置外层面板的尺寸
        messagePanel.setPreferredSize(new Dimension(Short.MAX_VALUE, userPreferredHeight + 80));
        messagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, userPreferredHeight + 80));

        messagePanel.add(leftSpacer, BorderLayout.WEST);
        messagePanel.add(rightPanel, BorderLayout.CENTER);
        messagePanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(JBColor.PanelBackground, 0.2f), 1),
                JBUI.Borders.empty(4, 8)));
        return messagePanel;
    }

    // 为了调整用户气泡的外框高度
    private int userPreferredHeight;

    // 创建自适应大小的文本区域
    private JTextArea createAutoSizingTextArea(String text) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(text);
        textArea.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));

        // 计算文本需要的尺寸
        FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = metrics.getHeight();

        // 设置最大宽度为聊天区域的合适大小
        int maxTextWidth = JBUI.scale(250); // 限制最大宽度
        int minTextWidth = JBUI.scale(20); // 设置最小宽度

        // 估算文本需要的宽度
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

        // 计算需要的行数（考虑自动换行）
        if (maxLineLength > maxTextWidth) {
            // 需要换行，重新计算行数
            int estimatedLines = 0;
            for (String line : lines) {
                int estimatedLineLength = (int) Math.ceil((double) metrics.stringWidth(line) / maxTextWidth);
                estimatedLines += Math.max(1, estimatedLineLength);
            }
            totalLines = estimatedLines;
            maxLineLength = maxTextWidth;
        }

        // 加上内边距
        int insetsWidth = textArea.getInsets().left + textArea.getInsets().right + JBUI.scale(24);
        int insetsHeight = textArea.getInsets().top + textArea.getInsets().bottom + JBUI.scale(16);

        // 设置最终尺寸
        int preferredWidth = Math.max(minTextWidth, Math.min(maxLineLength + insetsWidth, maxTextWidth + insetsWidth));
        int preferredHeight = Math.max(1, totalLines) * lineHeight + insetsHeight;

        userPreferredHeight = preferredHeight;

        textArea.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        textArea.setMaximumSize(new Dimension(preferredWidth, preferredHeight));

        return textArea;
    }

    // 创建AI消息气泡
    private JPanel createAiMessageBubble(String message) {
        // 主面板
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);

        // 设置对齐方式
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

        // 设置合理的宽度限制
        int viewportWidth = chatScrollPane != null ? chatScrollPane.getViewport().getWidth() : 400;
        int maxWidth = Math.max(200, viewportWidth - 60); // 确保最小宽度200像素
        messageText.setSize(new Dimension(maxWidth, 1));

        // 组装面板
        messagePanel.add(aiLabel, BorderLayout.NORTH);
        messagePanel.add(messageText, BorderLayout.CENTER);
        messagePanel.setBorder(JBUI.Borders.empty(0, 8, 0, 8));
        return messagePanel;
    }

    // 添加消息到聊天面板
    private void addMessageToChat(JPanel messagePanel, boolean scrollToBottom) {
        // 移除欢迎文本
        if (chatMessagesPanel.getComponentCount() > 0) {
            Component firstComponent = chatMessagesPanel.getComponent(0);
            if (firstComponent instanceof JLabel &&
                    ((JLabel) firstComponent).getText().contains("欢迎使用")) {
                chatMessagesPanel.removeAll();
            }
        }

        // 为每条消息添加固定的间距面板（除了第一条）
        if (chatMessagesPanel.getComponentCount() > 0) {
            JPanel fixedSpacer = new JPanel();
            fixedSpacer.setOpaque(false);
            fixedSpacer.setPreferredSize(new Dimension(0, MESSAGE_SPACING));
            fixedSpacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, MESSAGE_SPACING));
            fixedSpacer.setMinimumSize(new Dimension(0, MESSAGE_SPACING));
            // 设置固定高度，防止被BoxLayout重新计算
            fixedSpacer.setLayout(new BorderLayout());
            fixedSpacer.add(new JLabel(), BorderLayout.CENTER);
            chatMessagesPanel.add(fixedSpacer);
        }

        // 添加消息面板
        chatMessagesPanel.add(messagePanel);

        // 强制更新布局
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();

        // 简化的滚动逻辑
        if (scrollToBottom) {
            SwingUtilities.invokeLater(() -> {
                chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
            });
        }
    }

    // 显示思考中提示
    private void showThinkingIndicator() {
        // 创建思考提示面板
        thinkingPanel = new JPanel(new BorderLayout());
        thinkingPanel.setOpaque(false);
        thinkingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkingPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // 创建思考标签
        thinkingLabel = new JLabel("AI正在思考中");
        thinkingLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.ITALIC, SMALL_FONT_SIZE));
        thinkingLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        thinkingLabel.setBorder(JBUI.Borders.empty(4, 12, 4, 12));

        // 创建动画点
        StringBuilder dots = new StringBuilder(".");
        thinkingTimer = new Timer(500, e -> {
            dots.append(".");
            if (dots.length() > 3) {
                dots.setLength(1);
            }
            thinkingLabel.setText("小老师正在思考中" + dots.toString());
        });
        thinkingTimer.start();

        thinkingPanel.add(thinkingLabel, BorderLayout.CENTER);

        // 添加到聊天面板
        addMessageToChat(thinkingPanel, true);
    }

    // 隐藏思考中提示
    private void hideThinkingIndicator() {
        if (thinkingTimer != null && thinkingTimer.isRunning()) {
            thinkingTimer.stop();
        }

        if (thinkingPanel != null) {
            // 从聊天面板中移除思考提示
            chatMessagesPanel.remove(thinkingPanel);
            chatMessagesPanel.revalidate();
            chatMessagesPanel.repaint();
            thinkingPanel = null;
            thinkingLabel = null;
            thinkingTimer = null;
        }
    }

    // 开始AI响应
    private void startAiResponse() {
        // 隐藏思考中提示
        hideThinkingIndicator();

        // 创建AI文本区域
        currentAiMessage = new JTextArea();
        currentAiMessage.setEditable(false);
        currentAiMessage.setLineWrap(true);
        currentAiMessage.setWrapStyleWord(true);
        currentAiMessage.setOpaque(false);
        currentAiMessage.setForeground(JBUI.CurrentTheme.Label.foreground());
        currentAiMessage.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        currentAiMessage.setBorder(JBUI.Borders.empty(0, 8, 2, 8));
        currentAiMessage.setFocusable(false);

        // 确保文本区域不会过度扩展宽度
        currentAiMessage.setSize(new Dimension(Short.MAX_VALUE - 100, 1)); // 设置一个合理的初始宽度

        // 创建AI标签
        JLabel aiLabel = new JLabel("AI小老师 👨‍🏫");
        aiLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE));
        aiLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        aiLabel.setBorder(JBUI.Borders.empty(0, 8, 1, 8));

        // 创建AI消息容器 - 使用不同的布局策略
        currentAiMessagePanel = new JPanel(new BorderLayout());
        currentAiMessagePanel.setOpaque(false);

        // 设置对齐方式
        currentAiMessagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAiMessagePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // 不限制宽度，让BorderLayout自然处理换行
        currentAiMessagePanel.setPreferredSize(new Dimension(Short.MAX_VALUE, 50)); // 宽度不限
        currentAiMessagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Integer.MAX_VALUE)); // 宽度不限
        currentAiMessagePanel.setMinimumSize(new Dimension(200, 50)); // 最小宽度

        // 添加组件
        currentAiMessagePanel.add(aiLabel, BorderLayout.NORTH);
        currentAiMessagePanel.add(currentAiMessage, BorderLayout.CENTER);

        // 立即添加到聊天面板
        addMessageToChat(currentAiMessagePanel, true);
    }

    // 追加AI消息块
    private void appendAiMessageChunk(String chunk) {
        if (currentAiMessage != null) {
            SwingUtilities.invokeLater(() -> {
                // 添加文本内容
                currentAiMessage.append(chunk);

                // 强制文本区域重新计算大小和换行
                currentAiMessage.revalidate();
                currentAiMessage.repaint();

                // 确保文本区域正确换行
                currentAiMessage.setLineWrap(true);
                currentAiMessage.setWrapStyleWord(true);

                // 更新AI消息面板的大小，确保能容纳所有文本
                if (currentAiMessagePanel != null) {
                    // 保持之前设置的宽度限制
                    int maxWidth = currentAiMessagePanel.getPreferredSize().width;

                    // 计算文本区域需要的实际高度
                    int textHeight = currentAiMessage.getPreferredSize().height;
                    int totalHeight = textHeight + 50; // 加上标签和边距的高度
                    totalHeight = Math.max(50, totalHeight); // 确保最小高度

                    currentAiMessagePanel.setPreferredSize(new Dimension(maxWidth, totalHeight));
                    currentAiMessagePanel.setMaximumSize(new Dimension(maxWidth, totalHeight));
                    currentAiMessagePanel.setMinimumSize(new Dimension(200, totalHeight));

                    currentAiMessagePanel.revalidate();
                    currentAiMessagePanel.repaint();
                }

                // 更新整个聊天面板的布局
                chatMessagesPanel.revalidate();
                chatMessagesPanel.repaint();

                // 滚动到底部显示最新内容
                SwingUtilities.invokeLater(() -> {
                    JScrollBar scrollBar = chatScrollPane.getVerticalScrollBar();
                    scrollBar.setValue(scrollBar.getMaximum());
                });
            });
        }
    }

    // 完成AI响应
    private void finishAiResponse() {
        currentAiMessage = null;
        currentAiMessagePanel = null;
    }

    // 添加AI错误消息
    private void addAiErrorMessage(String error) {
        if (currentAiMessage != null) {
            currentAiMessage.append("\n[错误] " + error + "\n");
            currentAiMessage.setForeground(JBColor.RED);
        } else {
            JPanel errorPanel = createAiMessageBubble("[错误] " + error);
            addMessageToChat(errorPanel, true);
        }
        finishAiResponse();
    }

    // 构造函数
    public ChatToolWindowContent(Project project) {
        this.project = project;

        this.mainPanel = new JPanel(new BorderLayout());
        // 初始化 AI 客户端，每个项目使用唯一的 memoryId
        this.aiClient = new AiServiceClient(project.hashCode());

        // 初始化上下文服务
        this.contextService = ServiceManager.getService(project, ContextService.class);

        // 订阅上下文变更事件
        if (contextService != null) {
            contextService.addContextListener(newContext -> {
                SwingUtilities.invokeLater(() -> updateContextStatus());
            });
        }

        // 获取IDEA背景色
        Color ideBackgroundColor = JBColor.PanelBackground;
        Color lightBackgroundColor = lightenColor(ideBackgroundColor, 0.05f); // 浅5%
        Color inputBackgroundColor = lightenColor(ideBackgroundColor, 0.1f); // 输入框背景浅10%

        // 创建聊天显示区域
        chatMessagesPanel = new JPanel() {
            // 允许容器横向填充整个可用空间
            @Override
            public Dimension getMaximumSize() {
                Dimension size = super.getMaximumSize();
                return new Dimension(Short.MAX_VALUE, size.height);
            }
        };
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(lightBackgroundColor);

        // 设置BoxLayout的间距为0
        chatMessagesPanel.setBorder(JBUI.Borders.empty(0));

        // 设置主面板背景色
        mainPanel.setBackground(lightBackgroundColor);

        // 添加欢迎消息
        JLabel welcomeLabel = new JLabel("欢迎使用智能会话助手！");
        welcomeLabel.setFont(JBUI.Fonts.label());
        welcomeLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        welcomeLabel.setBorder(JBUI.Borders.empty(20, 8, 20, 50));
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE + 2));

        chatMessagesPanel.add(welcomeLabel);

        chatScrollPane = new JBScrollPane(chatMessagesPanel);
        chatScrollPane.setBackground(lightBackgroundColor);
        chatScrollPane.getViewport().setBackground(Color.WHITE);
        chatScrollPane.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(ideBackgroundColor, 0.2f), 1),
                JBUI.Borders.empty(4)));
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // 为滚动条添加平滑滚动
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 创建输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(lightBackgroundColor);

        JTextArea inputField = new JTextArea(1, 12); // 初始1行
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);

        // 设置最小和最大行数限制
        final int MIN_ROWS = 1;
        final int MAX_ROWS = 5; // 最多5行高度

        // 设置输入框颜色
        inputField.setBackground(inputBackgroundColor);
        inputField.setForeground(JBUI.CurrentTheme.Label.foreground());
        inputField.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, DEFAULT_FONT_SIZE));
        inputField.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(ideBackgroundColor, 0.2f), 1),
                JBUI.Borders.empty(5)));

        JBScrollPane inputScrollPane = new JBScrollPane(inputField);
        inputScrollPane.setBackground(inputBackgroundColor);
        inputScrollPane.getViewport().setBackground(inputBackgroundColor);

        JButton sendButton = new JButton("发送");
        sendButton.setBackground(new JBColor(new Color(66, 133, 244), new Color(45, 100, 200))); // 支持亮色/暗色主题
        // sendButton.setForeground(JBUI.CurrentTheme.Button.foreground());
        sendButton.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE));
        sendButton.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(ideBackgroundColor, 0.3f), 1),
                JBUI.Borders.empty(8, 16)));
        // sendButton.setPreferredSize(new Dimension(JBUI.scale(80), JBUI.scale(36)));
        // // 宽80，高36

        // 使用 InputMap 和 ActionMap 处理键盘快捷键
        InputMap inputMap = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = inputField.getActionMap();

        // Enter 键：发送消息
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        actionMap.put("send", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendButton.doClick();
            }
        });

        // Shift+Enter：换行
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");

        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(lightenColor(ideBackgroundColor, 0.2f), 1),
                JBUI.Borders.empty(8)));

        // 发送按钮事件
        sendButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                // 添加用户消息气泡
                JPanel userMessage = createUserMessageBubble(message);
                addMessageToChat(userMessage, true);

                inputField.setText("");
                inputField.setEnabled(false); // 发送时禁用输入框
                sendButton.setEnabled(false); // 禁用发送按钮

                // 显示思考中提示
                showThinkingIndicator();

                // 构建包含上下文的完整消息
                String fullMessage = message;
                if (contextService != null) {
                    String currentContext = contextService.getCurrentContext();
                    if (!currentContext.trim().isEmpty()) {
                        fullMessage = currentContext + "\n\n用户问题:\n" + message;
                    }
                }

                // 调用 AI 服务
                aiClient.sendMessage(
                        fullMessage,
                        // onChunk: 接收到数据块
                        chunk -> {
                            // 如果是第一个chunk，先开始AI响应
                            if (currentAiMessage == null) {
                                startAiResponse();
                            }
                            appendAiMessageChunk(chunk);
                        },
                        // onComplete: 完成
                        () -> {
                            finishAiResponse();
                            inputField.setEnabled(true); // 恢复输入框
                            sendButton.setEnabled(true); // 恢复发送按钮
                            inputField.requestFocus();
                        },
                        // onError: 出错
                        error -> {
                            hideThinkingIndicator(); // 隐藏思考提示
                            addAiErrorMessage(error);
                            inputField.setEnabled(true);
                            sendButton.setEnabled(true);
                            inputField.requestFocus();
                        });
            }
        });

        // 创建上下文状态显示
        contextStatusLabel = new JLabel("📝 上下文: 0 项");
        contextStatusLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        contextStatusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        contextStatusLabel.setBorder(JBUI.Borders.empty(4, 8, 4, 8));
        contextStatusLabel.setToolTipText("显示当前已添加到AI对话的代码上下文数量\n提示：在编辑器中选中代码后右键选择'添加到AI上下文'");

        // 初始化上下文状态
        updateContextStatus();

        // 组装界面
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(contextStatusLabel, BorderLayout.NORTH);

    }

    /**
     * 更新上下文状态显示
     */
    private void updateContextStatus() {
        if (contextService != null) {
            int contextCount = contextService.getContextList().size();
            String text = "📝 上下文: " + contextCount + " 项";

            if (contextCount > 0) {
                text += " (已激活)";
                contextStatusLabel.setForeground(new JBColor(new Color(0, 120, 215), new Color(100, 149, 237))); // 蓝色
            } else {
                contextStatusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
            }

            contextStatusLabel.setText(text);
        }
    }

    /**
     * 公共方法：发送消息到AI（可以从外部调用）
     * 
     * @param message 要发送的消息
     */
    public void sendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // 添加用户消息气泡
            JPanel userMessage = createUserMessageBubble(message.trim());
            addMessageToChat(userMessage, true);

            // 显示思考中提示
            showThinkingIndicator();

            // 构建包含上下文的完整消息
            String fullMessage = message.trim();
            if (contextService != null) {
                String currentContext = contextService.getCurrentContext();
                if (!currentContext.trim().isEmpty()) {
                    fullMessage = currentContext + "\n\n用户问题:\n" + message.trim();
                }
            }

            // 调用 AI 服务
            aiClient.sendMessage(
                    fullMessage,
                    // onChunk: 接收到数据块
                    chunk -> {
                        // 如果是第一个chunk，先开始AI响应
                        if (currentAiMessage == null) {
                            startAiResponse();
                        }
                        appendAiMessageChunk(chunk);
                    },
                    // onComplete: 完成
                    () -> {
                        finishAiResponse();
                    },
                    // onError: 出错
                    error -> {
                        hideThinkingIndicator(); // 隐藏思考提示
                        addAiErrorMessage(error);
                    });
        });
    }

    public JComponent getContent() {
        return mainPanel;
    }
}