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

        // 判断用户意图：是否需要修改代码
        boolean isModifyIntent = detectModifyIntent(message, contextList);

        if (isModifyIntent && contextList != null && !contextList.isEmpty()) {
            // 执行代码修改流程
            handleCodeModification(message, contextList);
        } else {
            // 执行普通对话流程
            handleNormalChat(message, contextList);
        }
    }

    /**
     * 检测用户意图是否为修改代码
     * 
     * @param message     用户消息
     * @param contextList 上下文列表
     * @return true表示用户意图为修改代码
     */
    private boolean detectModifyIntent(String message, java.util.List<ContextService.ContextItem> contextList) {
        // 如果没有代码上下文，不可能是修改代码
        if (contextList == null || contextList.isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // 检查是否有明确的命令前缀
        if (lowerMessage.startsWith("/modify ") || lowerMessage.startsWith("/refactor ")
                || lowerMessage.startsWith("/optimize ")) {
            return true;
        }

        // 关键词列表
        String[] modifyKeywords = {
                "修改", "优化", "重构", "添加", "删除", "改进",
                "修复", "fix", "refactor", "optimize", "add",
                "remove", "improve", "change", "update", "重写",
                "改成", "改为", "换成", "替换", "调整"
        };

        // 检查是否包含修改类关键词
        for (String keyword : modifyKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理代码修改流程
     * 
     * @param instruction 修改指令
     * @param contextList 上下文列表
     */
    private void handleCodeModification(String instruction, java.util.List<ContextService.ContextItem> contextList) {
        // 取第一个上下文作为要修改的代码
        ContextService.ContextItem codeItem = contextList.get(0);

        // 更新思考提示
        thinkingManager.updateMessage("AI正在修改代码...");

        // 清除上下文（因为已经使用了）
        if (contextService != null) {
            contextService.clearContext();
            updateContextStatus();
        }

        // 调用代码修改接口
        aiClient.modifyCodeWithDiff(
                codeItem.getContent(),
                instruction,
                codeItem.getFileName(),
                // onSuccess
                diffResult -> {
                    thinkingManager.hide();

                    if (diffResult.hasError()) {
                        responseHandler.addError("代码修改失败: " + diffResult.getError());
                        inputPanel.setInputEnabled(true);
                        inputPanel.requestInputFocus();
                        return;
                    }

                    if (!diffResult.hasChanges()) {
                        JPanel noChangePanel = bubbleFactory.createAiMessageBubble(
                                "**提示**: AI建议的代码与原代码相同，无需修改。");
                        messagePanel.addMessage(noChangePanel, true);
                        inputPanel.setInputEnabled(true);
                        inputPanel.requestInputFocus();
                        return;
                    }

                    // 在UI线程中处理
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            // 获取编辑器和偏移量（从聊天框场景）
                            com.intellij.openapi.editor.Editor editor = openFileAndGetEditor(codeItem);

                            if (editor == null) {
                                responseHandler.addError("无法打开编辑器，请手动应用修改");
                                inputPanel.setInputEnabled(true);
                                inputPanel.requestInputFocus();
                                return;
                            }

                            // 计算偏移量
                            com.intellij.openapi.editor.Document document = editor.getDocument();
                            int startLine = Math.max(0, codeItem.getStartLine() - 1);
                            int endLine = Math.max(0, codeItem.getEndLine() - 1);
                            int startOffset = document.getLineStartOffset(startLine);
                            int endOffset = document.getLineEndOffset(endLine);

                            // 打开差异查看器并获取虚拟文件
                            com.intellij.openapi.vfs.VirtualFile diffViewerFile = IntelliJDiffViewer
                                    .showDiffAndWaitForConfirmation(
                                            project, diffResult, editor, startOffset, endOffset);

                            if (diffViewerFile != null) {
                                // 使用PendingModificationManager管理修改
                                String modificationId = com.javaProgram.services.PendingModificationManager
                                        .addPendingModification(project, editor, diffResult,
                                                startOffset, endOffset, diffViewerFile);

                                // 显示统一的确认气泡（ModificationConfirmationPanel），传递文件名
                                String fileName = diffResult.getFileName() != null ? diffResult.getFileName()
                                        : codeItem.getFileName();
                                JPanel confirmationPanel = ModificationConfirmationPanel.create(modificationId,
                                        fileName);
                                messagePanel.addMessage(confirmationPanel, true);
                            }

                        } catch (Exception ex) {
                            System.err.println("处理代码修改失败: " + ex.getMessage());
                            ex.printStackTrace();
                            responseHandler.addError("处理代码修改失败: " + ex.getMessage());
                        }
                    });

                    // 重新启用输入
                    inputPanel.setInputEnabled(true);
                    inputPanel.requestInputFocus();
                },
                // onError
                error -> {
                    thinkingManager.hide();
                    responseHandler.addError("代码修改失败: " + error);
                    inputPanel.setInputEnabled(true);
                    inputPanel.requestInputFocus();
                });
    }

    /**
     * 打开文件并获取编辑器
     * 
     * @param codeItem 代码上下文项
     * @return 编辑器实例，失败返回null
     */
    private com.intellij.openapi.editor.Editor openFileAndGetEditor(ContextService.ContextItem codeItem) {
        try {
            String filePath = codeItem.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                System.err.println("文件路径为空");
                return null;
            }

            // 查找虚拟文件
            com.intellij.openapi.vfs.VirtualFile virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .findFileByIoFile(new java.io.File(filePath));

            if (virtualFile == null || !virtualFile.exists()) {
                System.err.println("找不到文件: " + filePath);
                return null;
            }

            // 打开文件并获取编辑器
            com.intellij.openapi.fileEditor.FileEditorManager editorManager = com.intellij.openapi.fileEditor.FileEditorManager
                    .getInstance(project);

            return editorManager.openTextEditor(
                    new com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile, 0), true);

        } catch (Exception e) {
            System.err.println("打开文件失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 处理普通对话流程
     * 
     * @param message     用户消息
     * @param contextList 上下文列表
     */
    private void handleNormalChat(String message, java.util.List<ContextService.ContextItem> contextList) {
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
        // 从PendingModificationManager获取修改信息，提取文件名
        com.javaProgram.services.PendingModificationManager.PendingModification modification = com.javaProgram.services.PendingModificationManager
                .getPendingModification(modificationId);

        String fileName = "未知";
        if (modification != null && modification.getDiffResult() != null) {
            fileName = modification.getDiffResult().getFileName();
            if (fileName == null) {
                fileName = "未知";
            }
        }

        JPanel panel = ModificationConfirmationPanel.create(modificationId, fileName);
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
