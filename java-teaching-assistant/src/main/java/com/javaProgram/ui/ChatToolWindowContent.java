package com.javaProgram.ui;

import com.javaProgram.services.AiServiceClient;
import com.javaProgram.services.ContextService;
import com.javaProgram.ui.components.*;
import com.javaProgram.ui.handlers.AiResponseHandler;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

/**
 * 聊天工具窗口内容（主控制器）
 * 职责：协调各个组件，处理顶层业务逻辑
 */
public class ChatToolWindowContent {
    private final JPanel mainPanel;
    private final Project project;

    // 服务层
    private final AiServiceClient aiClient;
    private final ContextService contextService;

    // UI组件
    private final MessageBubbleFactory bubbleFactory;
    private final ChatMessagePanel messagePanel;
    private final ChatInputPanel inputPanel;
    private final ContextDisplayPanel contextDisplayPanel;
    private final ThinkingIndicatorManager thinkingManager;

    // 处理器
    private final AiResponseHandler responseHandler;

    // 上下文状态
    private JLabel contextStatusLabel;

    public ChatToolWindowContent(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());

        // 初始化服务
        this.aiClient = new AiServiceClient(project.hashCode());
        this.contextService = ServiceManager.getService(project, ContextService.class);

        // 计算背景颜色
        Color backgroundColor = lightenColor(JBColor.PanelBackground, 0.05f);

        // 初始化UI组件
        this.messagePanel = new ChatMessagePanel(backgroundColor);
        this.bubbleFactory = new MessageBubbleFactory(messagePanel.getScrollPane(), project);
        this.inputPanel = new ChatInputPanel(backgroundColor, project, contextService);
        this.contextDisplayPanel = new ContextDisplayPanel(contextService, project);
        this.thinkingManager = new ThinkingIndicatorManager(bubbleFactory, messagePanel);
        this.responseHandler = new AiResponseHandler(bubbleFactory, messagePanel);

        // 设置输入框回调
        inputPanel.setOnSendMessage(this::handleSendMessage);
        // 设置上下文添加回调
        inputPanel.setOnContextAdded(this::updateContextStatus);

        // 订阅上下文变更
        if (contextService != null) {
            contextService.addContextListener(ctx -> SwingUtilities.invokeLater(this::updateContextStatus));
        }

        // 组装UI
        assembleUI(backgroundColor);
    }

    /**
     * 组装UI界面
     */
    private void assembleUI(Color backgroundColor) {
        mainPanel.setBackground(backgroundColor);

        // 上下文状态标签
        contextStatusLabel = new JLabel("📝 上下文: 0 项");
        contextStatusLabel.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, 12f));
        contextStatusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        contextStatusLabel.setBorder(JBUI.Borders.empty(4, 8, 4, 8));
        contextStatusLabel.setToolTipText("显示当前已添加到AI对话的代码上下文数量\n提示：在编辑器中选中代码后右键选择'添加到AI上下文'");

        // 输入区域容器
        JPanel inputAreaContainer = new JPanel(new BorderLayout());
        inputAreaContainer.setBackground(backgroundColor);
        inputAreaContainer.add(contextDisplayPanel, BorderLayout.NORTH);
        inputAreaContainer.add(inputPanel, BorderLayout.CENTER);

        // 组装主面板
        mainPanel.add(contextStatusLabel, BorderLayout.NORTH);
        mainPanel.add(messagePanel.getScrollPane(), BorderLayout.CENTER);
        mainPanel.add(inputAreaContainer, BorderLayout.SOUTH);

        updateContextStatus();
    }

    /**
     * 处理发送消息
     */
    private void handleSendMessage(String message) {
        // 📌 在添加用户消息前，先获取当前上下文列表（因为后面会清除）
        var contextList = contextService != null ? contextService.getContextList() : null;

        // 添加用户消息（带上下文信息）
        JPanel userBubble = bubbleFactory.createUserMessageBubble(message, contextList);
        messagePanel.addMessage(userBubble, true);

        // 禁用输入
        inputPanel.setInputEnabled(false);

        // 显示思考提示
        thinkingManager.show();

        // 构建完整消息（包含上下文）
        String fullMessage = buildFullMessage(message);

        // 📌 发送后清除上下文（因为已经包含在消息中了）
        if (contextService != null) {
            contextService.clearContext();
            updateContextStatus();
        }

        // 调用AI服务（传递项目路径，让AI能自主读取代码）
        String projectPath = project.getBasePath(); // 获取项目根目录
        aiClient.sendMessage(
                fullMessage,
                projectPath, // 传递项目路径给后端
                // onChunk
                chunk -> {
                    if (responseHandler.isIdle()) {
                        thinkingManager.hide();
                        responseHandler.startResponse();
                    }
                    responseHandler.appendChunk(chunk);
                },
                // onComplete
                () -> {
                    responseHandler.finishResponse();
                    inputPanel.setInputEnabled(true);
                    inputPanel.requestInputFocus();
                },
                // onError
                error -> {
                    thinkingManager.hide();
                    responseHandler.addError(error);
                    inputPanel.setInputEnabled(true);
                    inputPanel.requestInputFocus();
                });
    }

    /**
     * 构建包含上下文的完整消息
     */
    private String buildFullMessage(String userMessage) {
        if (contextService != null) {
            String context = contextService.getCurrentContext();
            if (!context.trim().isEmpty()) {
                return context + "\n\n用户问题:\n" + userMessage;
            }
        }
        return userMessage;
    }

    /**
     * 更新上下文状态显示
     */
    private void updateContextStatus() {
        if (contextService != null) {
            var contextList = contextService.getContextList();
            int count = contextList.size();
            String text = "📝 上下文: " + count + " 项";

            if (count > 0) {
                text += " (已激活)";
                contextStatusLabel.setForeground(
                        new JBColor(new Color(0, 120, 215), new Color(100, 149, 237)));
            } else {
                contextStatusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
            }

            contextStatusLabel.setText(text);
            contextDisplayPanel.updateContextDisplay(contextList);
        }
    }

    /**
     * 添加修改确认消息（公共API）
     */
    public void addModificationConfirmationMessage(String modificationId) {
        JPanel panel = ModificationConfirmationPanel.create(modificationId);
        messagePanel.addMessage(panel, true);
    }

    /**
     * 发送消息（公共API）
     */
    public void sendMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> handleSendMessage(message.trim()));
        }
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

    public JComponent getContent() {
        return mainPanel;
    }
}
