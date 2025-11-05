package com.javaProgram.ui;

import com.javaProgram.services.AiServiceClient;
import com.javaProgram.services.ContextService;
import com.javaProgram.services.PendingModificationManager;
import com.javaProgram.ui.components.ContextDisplayPanel;
import com.javaProgram.ui.components.MessageBubbleFactory;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

    // 上下文显示面板
    private ContextDisplayPanel contextDisplayPanel;
    
    // 消息气泡工厂
    private MessageBubbleFactory messageBubbleFactory;

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
        thinkingLabel.setFont(JBUI.Fonts.label().deriveFont(Font.ITALIC, SMALL_FONT_SIZE));
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
        currentAiMessage.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        currentAiMessage.setBorder(JBUI.Borders.empty(0, 8, 2, 8));
        currentAiMessage.setFocusable(false);

        // 确保文本区域不会过度扩展宽度
        currentAiMessage.setSize(new Dimension(Short.MAX_VALUE - 100, 1)); // 设置一个合理的初始宽度

        // 创建AI标签
        JLabel aiLabel = new JLabel("AI小老师 👨‍🏫");
        aiLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE));
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
            JPanel errorPanel = messageBubbleFactory.createAiMessageBubble("[错误] " + error);
            addMessageToChat(errorPanel, true);
        }
        finishAiResponse();
    }

    // 添加修改确认消息
    public void addModificationConfirmationMessage(String modificationId) {
        // 创建包含确认按钮的面板
        JPanel confirmationPanel = createModificationConfirmationPanel(modificationId);
        addMessageToChat(confirmationPanel, true);
    }

    // 创建修改确认面板
    private JPanel createModificationConfirmationPanel(String modificationId) {
        // 主面板 - 使用BoxLayout以确保不限制后续消息
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.Y_AXIS));
        wrapperPanel.setOpaque(false);
        wrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 内部消息面板 - 固定尺寸
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setAlignmentY(Component.TOP_ALIGNMENT);
        messagePanel.setMaximumSize(new Dimension(JBUI.scale(400), JBUI.scale(120)));
        messagePanel.setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(120)));
        messagePanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.BLUE, 1),
                JBUI.Borders.empty(8)
        ));
        messagePanel.setBackground(JBUI.CurrentTheme.ToolWindow.background());

        // AI标签
        JLabel aiLabel = new JLabel("AI小老师 👨‍🏫");
        aiLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE));
        aiLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        aiLabel.setBorder(JBUI.Borders.empty(1, 8, 1, 8));

        // 消息内容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        // 消息文本
        JTextArea messageText = new JTextArea("代码修改已完成！\n差异对比已在IntelliJ中显示。\n\n是否应用此修改？");
        messageText.setEditable(false);
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setOpaque(false);
        messageText.setForeground(JBUI.CurrentTheme.Label.foreground());
        messageText.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        messageText.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
        messageText.setFocusable(false);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(JBUI.Borders.empty(0, 8, 8, 8));

        // 接受按钮
        JButton acceptButton = new JButton("✓ 接受修改");
        acceptButton.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        acceptButton.setForeground(JBColor.GREEN);
        acceptButton.setOpaque(false);
        acceptButton.setContentAreaFilled(false);
        acceptButton.setBorder(BorderFactory.createLineBorder(JBColor.GREEN, 1));
        acceptButton.setFocusPainted(false);
        acceptButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // 保存原始尺寸和字体大小，用于恢复
        final Dimension originalSize = acceptButton.getPreferredSize();
        final Font originalFont = acceptButton.getFont();

        acceptButton.addActionListener(e -> {
            // 应用修改
            PendingModificationManager.applyModification(modificationId);
            // 更新消息为已接受状态 - 传递wrapperPanel而不是messagePanel
            updateModificationStatus(wrapperPanel, "✅ 修改已成功应用到编辑器！", JBColor.GREEN);
        });
        acceptButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 颜色变深
                acceptButton.setForeground(new Color(0,204,102));
                acceptButton.setBorder(BorderFactory.createLineBorder(new Color(153,255,153), 1));
                // 按钮稍微放大（通过增加字体大小实现）
                Font largerFont = originalFont.deriveFont(Font.PLAIN, MINI_FONT_SIZE + 1);
                acceptButton.setFont(largerFont);
                // 重新计算并设置按钮大小
                acceptButton.setPreferredSize(new Dimension(
                        (int)(originalSize.width * 1.05),
                        (int)(originalSize.height * 1.1)
                ));
                acceptButton.revalidate();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                // 恢复原始颜色
                acceptButton.setForeground(JBColor.GREEN);
                acceptButton.setBorder(BorderFactory.createLineBorder(JBColor.GREEN, 1));
                // 恢复原始大小和字体
                acceptButton.setFont(originalFont);
                acceptButton.setPreferredSize(originalSize);
                acceptButton.revalidate();
            }
        });

        // 拒绝按钮
        JButton rejectButton = new JButton("✗ 拒绝修改");
        rejectButton.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        rejectButton.setForeground(JBColor.RED);
        rejectButton.setOpaque(false);
        rejectButton.setContentAreaFilled(false);
        rejectButton.setBorder(BorderFactory.createLineBorder(JBColor.RED, 1));
        rejectButton.setFocusPainted(false);
        rejectButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rejectButton.addActionListener(e -> {
            // 拒绝修改
            PendingModificationManager.rejectModification(modificationId);
            // 更新消息为已拒绝状态 - 传递wrapperPanel而不是messagePanel
            updateModificationStatus(wrapperPanel, "❌ 修改已取消", JBColor.RED);
        });

        rejectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 颜色变深
                rejectButton.setForeground(new Color(255,51,51));
                rejectButton.setBorder(BorderFactory.createLineBorder(new Color(204,0,0), 1));
                // 按钮稍微放大（通过增加字体大小实现）
                Font largerFont = originalFont.deriveFont(Font.PLAIN, MINI_FONT_SIZE + 1);
                rejectButton.setFont(largerFont);
                // 重新计算并设置按钮大小
                rejectButton.setPreferredSize(new Dimension(
                        (int)(originalSize.width * 1.05),
                        (int)(originalSize.height * 1.1)
                ));
                rejectButton.revalidate();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                // 恢复原始颜色
                rejectButton.setForeground(JBColor.RED);
                rejectButton.setBorder(BorderFactory.createLineBorder(JBColor.RED, 1));
                // 恢复原始大小和字体
                rejectButton.setFont(originalFont);
                rejectButton.setPreferredSize(originalSize);
                rejectButton.revalidate();
            }
        });

        buttonPanel.add(acceptButton);
        buttonPanel.add(Box.createHorizontalStrut(JBUI.scale(10)));
        buttonPanel.add(rejectButton);

        // 组装面板
        contentPanel.add(messageText, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        messagePanel.add(aiLabel, BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);

        // 保存按钮引用以便后续更新状态
        messagePanel.putClientProperty("acceptButton", acceptButton);
        messagePanel.putClientProperty("rejectButton", rejectButton);

        // 将内部面板包装到外部包装器中
        wrapperPanel.add(messagePanel);

        return wrapperPanel;
    }

    // 更新修改状态
    private void updateModificationStatus(JPanel wrapperPanel, String statusText, Color statusColor) {
        // 获取内部消息面板
        if (wrapperPanel.getComponentCount() > 0) {
            Component firstComponent = wrapperPanel.getComponent(0);
            if (firstComponent instanceof JPanel) {
                JPanel messagePanel = (JPanel) firstComponent;

                // 找到按钮面板并替换为状态标签
                Component[] components = messagePanel.getComponents();
                for (Component component : components) {
                    if (component instanceof JPanel && component != messagePanel.getComponent(0)) { // 找到内容面板
                        JPanel contentPanel = (JPanel) component;
                        Component[] contentComponents = contentPanel.getComponents();

                        // 遍历内容面板的组件
                        for (int i = 0; i < contentComponents.length; i++) {
                            Component contentComponent = contentComponents[i];

                            // 找到按钮面板，替换为状态标签
                            if (contentComponent instanceof JPanel) {
                                JPanel buttonPanel = (JPanel) contentComponent;

                                // 创建状态标签
                                JLabel statusLabel = new JLabel(statusText);
                                statusLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, SMALL_FONT_SIZE));
                                statusLabel.setForeground(statusColor);
                                statusLabel.setBorder(JBUI.Borders.empty(0, 8, 0, 8));
                                statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                                // 替换按钮面板为状态标签
                                contentPanel.remove(i);
                                contentPanel.add(statusLabel, BorderLayout.CENTER);
                                break;
                            }
                        }

                        // 更新主消息文本
                        for (Component contentComponent : contentComponents) {
                            if (contentComponent instanceof JTextArea) {
                                JTextArea textArea = (JTextArea) contentComponent;
                                textArea.setText("AI代码修改：");
                                textArea.setForeground(JBUI.CurrentTheme.Label.foreground());
                                break;
                            }
                        }
                        break;
                    }
                }

                // 更新边框颜色以反映状态
                messagePanel.setBorder(JBUI.Borders.compound(
                        JBUI.Borders.customLine(statusColor, 1),
                        JBUI.Borders.empty(8)
                ));

                // 调整面板高度以适应新内容
                messagePanel.setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(100)));
                messagePanel.setMaximumSize(new Dimension(JBUI.scale(400), JBUI.scale(100)));

                // 刷新显示
                wrapperPanel.revalidate();
                wrapperPanel.repaint();
            }
        }
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
        
        // 初始化消息气泡工厂
        messageBubbleFactory = new MessageBubbleFactory(chatScrollPane);

        // 创建输入区域容器（包含上下文显示和输入框）
        JPanel inputAreaContainer = new JPanel(new BorderLayout());
        inputAreaContainer.setBackground(lightBackgroundColor);

        // 创建上下文显示面板
        contextDisplayPanel = new ContextDisplayPanel(contextService, project);

        // 创建输入面板
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(lightBackgroundColor);

        // 创建发送按钮 - 使用箭头图标
        JButton sendButton = new JButton("→");
        sendButton.setBackground(new JBColor(new Color(66, 133, 244), new Color(45, 100, 200)));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Arial", Font.BOLD, 18));
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(true);
        sendButton.setOpaque(true);
        
        // 设置固定大小
        Dimension buttonSize = new Dimension(JBUI.scale(32), JBUI.scale(32));
        sendButton.setPreferredSize(buttonSize);
        sendButton.setMinimumSize(buttonSize);
        sendButton.setMaximumSize(buttonSize);
        
        // 鼠标悬停效果
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new JBColor(new Color(51, 103, 214), new Color(35, 80, 180)));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new JBColor(new Color(66, 133, 244), new Color(45, 100, 200)));
            }
        });

        JTextArea inputField = new JTextArea(2, 12); // 初始2行
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);

        // 设置输入框颜色，右边留出按钮的空间
        inputField.setBackground(inputBackgroundColor);
        inputField.setForeground(JBUI.CurrentTheme.Label.foreground());
        inputField.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, DEFAULT_FONT_SIZE));
        // 右边留出40像素给按钮
        inputField.setBorder(JBUI.Borders.empty(5, 8, 5, 45));

        JBScrollPane inputScrollPane = new JBScrollPane(inputField);
        inputScrollPane.setBackground(inputBackgroundColor);
        inputScrollPane.getViewport().setBackground(inputBackgroundColor);
        inputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScrollPane.setBorder(JBUI.Borders.customLine(lightenColor(ideBackgroundColor, 0.2f), 1));
        
        // 设置输入框的初始首选高度（基于行数）
        FontMetrics fm = inputField.getFontMetrics(inputField.getFont());
        int lineHeight = fm.getHeight();
        int defaultRows = 2; // 默认显示2行
        int defaultHeight = lineHeight * defaultRows + inputField.getInsets().top + inputField.getInsets().bottom + JBUI.scale(20);
        inputScrollPane.setPreferredSize(new Dimension(300, defaultHeight));

        // 创建输入框容器，使用JLayeredPane确保按钮在最上层
        JLayeredPane inputFieldContainer = new JLayeredPane() {
            @Override
            public Dimension getPreferredSize() {
                // 基于输入框的首选大小
                Dimension scrollPaneSize = inputScrollPane.getPreferredSize();
                return new Dimension(scrollPaneSize.width, Math.max(scrollPaneSize.height, defaultHeight));
            }
        };
        inputFieldContainer.setBackground(inputBackgroundColor);
        
        // 添加组件到不同的层
        inputFieldContainer.add(inputScrollPane, JLayeredPane.DEFAULT_LAYER);
        inputFieldContainer.add(sendButton, JLayeredPane.PALETTE_LAYER); // 按钮在更高的层
        
        // 监听容器大小变化，动态调整组件位置
        inputFieldContainer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int containerWidth = inputFieldContainer.getWidth();
                int containerHeight = inputFieldContainer.getHeight();
                
                // 输入框占据整个容器
                inputScrollPane.setBounds(0, 0, containerWidth, containerHeight);
                
                // 按钮定位到右下角
                int buttonWidth = sendButton.getPreferredSize().width;
                int buttonHeight = sendButton.getPreferredSize().height;
                int buttonX = containerWidth - buttonWidth - JBUI.scale(4);
                int buttonY = containerHeight - buttonHeight - JBUI.scale(4);
                sendButton.setBounds(buttonX, buttonY, buttonWidth, buttonHeight);
            }
        });

        // 实现输入框高度自适应（最高不超过窗口高度的30%）
        inputField.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateInputFieldHeight(inputField, inputScrollPane);
            }
        });

        // 监听文本变化，动态调整输入框高度
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateInputFieldHeight(inputField, inputScrollPane));
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateInputFieldHeight(inputField, inputScrollPane));
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateInputFieldHeight(inputField, inputScrollPane));
            }
        });

        // 添加悬浮效果 - 保存原始状态
        final Color originalBackground = sendButton.getBackground();
        final Font originalFont = sendButton.getFont();
        final Border originalBorder = sendButton.getBorder();
        final Cursor originalCursor = sendButton.getCursor();

        // 设置鼠标手型光标
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 创建状态管理器来控制悬浮效果
        class HoverEffectController {
            boolean isEnabled = true;

            public void setEnabled(boolean enabled) {
                this.isEnabled = enabled;
            }
        }
        final HoverEffectController hoverController = new HoverEffectController();

        // 添加鼠标事件监听器
        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 只有在悬浮效果启用且按钮可用时才显示悬浮效果
                if (hoverController.isEnabled && sendButton.isEnabled()) {
                    sendButton.setBackground(lightenColor(ideBackgroundColor, 0.4f)); // 背景色变深
                    sendButton.setFont(originalFont.deriveFont(Font.BOLD, DEFAULT_FONT_SIZE + 1)); // 字体稍微放大
                    sendButton.setBorder(JBUI.Borders.compound(
                            JBUI.Borders.customLine(lightenColor(ideBackgroundColor, 0.5f), 1), // 边框颜色变深
                            JBUI.Borders.empty(8, 16)
                    ));
                    sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 确保手型光标
                    sendButton.revalidate();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 只有在悬浮效果启用时才恢复原始状态
                if (hoverController.isEnabled) {
                    sendButton.setBackground(originalBackground);
                    sendButton.setFont(originalFont);
                    sendButton.setBorder(originalBorder);
                    sendButton.setCursor(originalCursor);
                    sendButton.revalidate();
                }
            }
        });

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

        inputPanel.add(inputFieldContainer, BorderLayout.CENTER);
        inputPanel.setBorder(JBUI.Borders.empty(4));

        // 组装输入区域容器
        inputAreaContainer.add(contextDisplayPanel, BorderLayout.NORTH);
        inputAreaContainer.add(inputPanel, BorderLayout.CENTER);

        // 发送按钮事件
        sendButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                // 添加用户消息气泡
                JPanel userMessage = messageBubbleFactory.createUserMessageBubble(message);
                addMessageToChat(userMessage, true);

                inputField.setText("");
                inputField.setEnabled(false); // 发送时禁用输入框
                sendButton.setEnabled(false); // 禁用发送按钮
                hoverController.setEnabled(false); // 禁用悬浮效果
                sendButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // 恢复默认光标

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
                            hoverController.setEnabled(true); // 恢复悬浮效果
                            sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 恢复手型光标
                            inputField.requestFocus();
                        },
                        // onError: 出错
                        error -> {
                            hideThinkingIndicator(); // 隐藏思考提示
                            addAiErrorMessage(error);
                            inputField.setEnabled(true);
                            sendButton.setEnabled(true);
                            hoverController.setEnabled(true); // 恢复悬浮效果
                            sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 恢复手型光标
                            inputField.requestFocus();
                        });
            }
        });

        // 创建上下文状态显示
        contextStatusLabel = new JLabel("📝 上下文: 0 项");
        contextStatusLabel.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        contextStatusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        contextStatusLabel.setBorder(JBUI.Borders.empty(4, 8, 4, 8));
        contextStatusLabel.setToolTipText("显示当前已添加到AI对话的代码上下文数量\n提示：在编辑器中选中代码后右键选择'添加到AI上下文'");

        // 初始化上下文状态
        updateContextStatus();

        // 组装界面
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputAreaContainer, BorderLayout.SOUTH);
        mainPanel.add(contextStatusLabel, BorderLayout.NORTH);

    }

    /**
     * 更新输入框高度以适应文本内容
     */
    private void updateInputFieldHeight(JTextArea inputField, JBScrollPane scrollPane) {
        try {
            // 获取主面板的高度
            int mainPanelHeight = mainPanel.getHeight();

            // 如果主面板高度为0（还没有完全初始化），使用默认值
            if (mainPanelHeight <= 0) {
                mainPanelHeight = 600; // 默认高度
            }

            // 计算最大允许高度（窗口高度的30%）
            int maxHeight = (int) (mainPanelHeight * 0.3);

            // 确保最大高度至少有一个合理的值
            maxHeight = Math.max(maxHeight, JBUI.scale(150));

            // 计算单行高度
            FontMetrics fm = inputField.getFontMetrics(inputField.getFont());
            int lineHeight = fm.getHeight();

            // 计算实际显示的行数（包括自动换行）
            int actualLines = calculateActualLineCount(inputField);

            // 计算实际需要的高度
            int contentHeight = actualLines * lineHeight + inputField.getInsets().top + inputField.getInsets().bottom
                    + JBUI.scale(10);

            // 设置最小高度（1行）和最大高度（30%）
            int minHeight = lineHeight * 1 + inputField.getInsets().top + inputField.getInsets().bottom
                    + JBUI.scale(10);
            int targetHeight = Math.max(minHeight, Math.min(contentHeight, maxHeight));

            // 只有在高度变化时才更新
            if (Math.abs(scrollPane.getPreferredSize().height - targetHeight) > 2) {
                // 更新滚动面板的首选高度
                Dimension preferredSize = new Dimension(scrollPane.getPreferredSize().width, targetHeight);
                scrollPane.setPreferredSize(preferredSize);
                scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, targetHeight));
                scrollPane.setMinimumSize(new Dimension(100, minHeight));

                // 刷新布局 - 需要刷新父容器及其父容器
                Container parent = scrollPane.getParent();
                if (parent != null) {
                    parent.revalidate();
                    Container grandParent = parent.getParent();
                    if (grandParent != null) {
                        grandParent.revalidate();
                    }
                }
                scrollPane.revalidate();
            }
        } catch (Exception ex) {
            // 忽略异常
        }
    }

    /**
     * 计算文本区域实际显示的行数（包括自动换行）
     */
    private int calculateActualLineCount(JTextArea textArea) {
        try {
            String text = textArea.getText();
            if (text.isEmpty()) {
                return 1;
            }

            // 获取文本区域的宽度
            int width = textArea.getWidth();
            if (width <= 0) {
                width = textArea.getParent().getWidth() - JBUI.scale(100); // 减去滚动条和边距
            }
            if (width <= 0) {
                width = JBUI.scale(200); // 默认宽度
            }

            // 减去边距
            width = width - textArea.getInsets().left - textArea.getInsets().right;

            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            String[] lines = text.split("\n", -1);
            int totalLines = 0;

            for (String line : lines) {
                if (line.isEmpty()) {
                    totalLines++;
                } else {
                    // 计算这一行需要多少显示行
                    int lineWidth = fm.stringWidth(line);
                    int wrappedLines = (int) Math.ceil((double) lineWidth / width);
                    totalLines += Math.max(1, wrappedLines);
                }
            }

            return Math.max(1, totalLines);
        } catch (Exception e) {
            return textArea.getLineCount(); // 发生异常时使用简单的行数计算
        }
    }

    /**
     * 更新上下文状态显示
     */
    private void updateContextStatus() {
        if (contextService != null) {
            java.util.List<ContextService.ContextItem> contextList = contextService.getContextList();
            int contextCount = contextList.size();
            String text = "📝 上下文: " + contextCount + " 项";

            if (contextCount > 0) {
                text += " (已激活)";
                contextStatusLabel.setForeground(new JBColor(new Color(0, 120, 215), new Color(100, 149, 237))); // 蓝色
            } else {
                contextStatusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
            }

            contextStatusLabel.setText(text);

            // 更新上下文显示面板
            contextDisplayPanel.updateContextDisplay(contextList);
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
            JPanel userMessage = messageBubbleFactory.createUserMessageBubble(message.trim());
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