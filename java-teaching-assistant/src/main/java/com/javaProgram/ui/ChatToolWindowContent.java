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
    private final com.javaProgram.services.RequestQueueManager queueManager;

    // UI组件
    private final MessageBubbleFactory bubbleFactory;
    private final ChatMessagePanel messagePanel;
    private final ChatInputPanel inputPanel;
    private final ContextDisplayPanel contextDisplayPanel;
    private final QueueDisplayPanel queueDisplayPanel;
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

        // 初始化请求队列管理器
        this.queueManager = new com.javaProgram.services.RequestQueueManager();
        queueManager.setOnProcessRequest(this::executeRequest);

        // 计算背景颜色
        Color backgroundColor = lightenColor(JBColor.PanelBackground, 0.05f);

        // 初始化UI组件
        this.messagePanel = new ChatMessagePanel(backgroundColor);
        this.bubbleFactory = new MessageBubbleFactory(messagePanel.getScrollPane(), project);
        this.inputPanel = new ChatInputPanel(backgroundColor, project, contextService);
        this.contextDisplayPanel = new ContextDisplayPanel(contextService, project);
        this.queueDisplayPanel = new QueueDisplayPanel(queueManager);
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

        // 创建上下文和队列的容器
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(backgroundColor);
        topContainer.add(queueDisplayPanel, BorderLayout.NORTH);
        topContainer.add(contextDisplayPanel, BorderLayout.CENTER);

        inputAreaContainer.add(topContainer, BorderLayout.NORTH);
        inputAreaContainer.add(inputPanel, BorderLayout.CENTER);

        // 组装主面板
        mainPanel.add(contextStatusLabel, BorderLayout.NORTH);
        mainPanel.add(messagePanel.getScrollPane(), BorderLayout.CENTER);
        mainPanel.add(inputAreaContainer, BorderLayout.SOUTH);

        updateContextStatus();
    }

    /**
     * 处理发送消息（队列模式）
     */
    private void handleSendMessage(String message) {
        // 获取当前上下文列表（会创建副本保存到队列中）
        var contextList = contextService != null ? contextService.getContextList() : null;

        // 尝试加入队列
        boolean added = queueManager.addRequest(message, contextList);

        if (!added) {
            // 队列已满，提示用户
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "请求队列已满（最多3个），请稍后再试",
                    "队列已满",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 清空输入框但不禁用
        inputPanel.clearInput();

        // 如果没有正在处理的请求，立即开始处理
        if (!queueManager.isProcessing()) {
            queueManager.startProcessing();
        }
    }

    /**
     * 实际执行请求（由队列管理器回调）
     * 
     * @param request 待处理的请求
     */
    private void executeRequest(com.javaProgram.services.RequestQueueManager.QueuedRequest request) {
        String message = request.getMessage();
        var contextList = request.getContextList();

        // 在UI线程中执行
        SwingUtilities.invokeLater(() -> {
            // 在实际开始处理时才显示用户消息气泡
            JPanel userBubble = bubbleFactory.createUserMessageBubble(message, contextList);
            messagePanel.addMessage(userBubble, true);

            // 显示思考提示
            thinkingManager.show();

            // 清除上下文服务（因为请求已经保存了上下文副本）
            if (contextService != null) {
                contextService.clearContext();
                updateContextStatus();
            }

            // 根据上下文判断意图
            if (contextList != null && !contextList.isEmpty()) {
                detectModifyIntentWithAI(message, contextList);
            } else {
                handleNormalChat(message, contextList);
            }
        });
    }

    /**
     * 使用AI进行意图识别
     * 
     * @param message     用户消息
     * @param contextList 上下文列表
     */
    private void detectModifyIntentWithAI(String message, java.util.List<ContextService.ContextItem> contextList) {
        // 调用AI服务进行意图识别
        aiClient.detectIntent(
                message,
                // onSuccess - AI返回意图
                intent -> {
                    System.out.println("AI意图识别结果: " + intent);
                    if ("modify".equals(intent)) {
                        // 执行代码修改流程
                        handleCodeModification(message, contextList);
                    } else {
                        // 执行普通对话流程
                        handleNormalChat(message, contextList);
                    }
                },
                // onError - AI识别失败，使用关键词匹配作为备用方案
                error -> {
                    System.err.println("AI意图识别失败，使用关键词匹配备用方案: " + error);
                    boolean isModifyIntent = detectModifyIntentWithKeywords(message);
                    if (isModifyIntent) {
                        handleCodeModification(message, contextList);
                    } else {
                        handleNormalChat(message, contextList);
                    }
                });
    }

    /**
     * 使用关键词检测用户意图（备用方案）
     * 
     * @param message 用户消息
     * @return true表示用户意图为修改代码
     */
    private boolean detectModifyIntentWithKeywords(String message) {
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
     * 处理代码修改流程（支持多文件，串行处理保证顺序）
     * 
     * @param instruction 修改指令
     * @param contextList 上下文列表
     */
    private void handleCodeModification(String instruction, java.util.List<ContextService.ContextItem> contextList) {
        // 获取要修改的文件数量
        int totalFiles = contextList.size();

        // 更新思考提示
        thinkingManager.updateMessage("AI正在修改 " + totalFiles + " 个文件...");

        // 使用原子计数器追踪成功数量
        final java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // 从第一个文件开始串行处理
        processFileModification(instruction, contextList, 0, totalFiles, successCount);
    }

    /**
     * 递归处理单个文件的修改（串行保证顺序）
     * 
     * @param instruction  修改指令
     * @param contextList  上下文列表
     * @param currentIndex 当前处理的文件索引
     * @param totalFiles   总文件数
     * @param successCount 成功修改的文件计数器
     */
    private void processFileModification(String instruction,
            java.util.List<ContextService.ContextItem> contextList,
            int currentIndex,
            int totalFiles,
            java.util.concurrent.atomic.AtomicInteger successCount) {

        // 如果所有文件都处理完了
        if (currentIndex >= totalFiles) {
            thinkingManager.hide();

            // 显示完成摘要
            if (successCount.get() > 0) {
                JPanel summaryPanel = bubbleFactory.createAiMessageBubble(
                        "✅ **代码修改完成**: 成功修改 " + successCount.get() + "/" + totalFiles + " 个文件");
                messagePanel.addMessage(summaryPanel, true);
            }

            // 通知队列管理器完成
            queueManager.completeCurrentRequest();
            inputPanel.requestInputFocus();
            return;
        }

        final ContextService.ContextItem codeItem = contextList.get(currentIndex);
        final int fileIndex = currentIndex + 1;

        // 处理当前文件
        aiClient.modifyCodeWithDiff(
                codeItem.getContent(),
                instruction,
                codeItem.getFileName(),
                // onSuccess
                diffResult -> {
                    if (diffResult.hasError()) {
                        responseHandler.addError("文件 [" + codeItem.getFileName() + "] 修改失败: " + diffResult.getError());
                        // 继续处理下一个文件
                        processFileModification(instruction, contextList, currentIndex + 1, totalFiles, successCount);
                    } else if (!diffResult.hasChanges()) {
                        JPanel noChangePanel = bubbleFactory.createAiMessageBubble(
                                "**文件 " + fileIndex + "/" + totalFiles + "**: `" + codeItem.getFileName() +
                                        "` - AI建议的代码与原代码相同，无需修改。");
                        messagePanel.addMessage(noChangePanel, true);
                        // 继续处理下一个文件
                        processFileModification(instruction, contextList, currentIndex + 1, totalFiles, successCount);
                    } else {
                        // 有修改内容，处理diff
                        successCount.incrementAndGet();

                        // 在UI线程中处理
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                // 获取编辑器和偏移量
                                com.intellij.openapi.editor.Editor editor = openFileAndGetEditor(codeItem);

                                if (editor == null) {
                                    responseHandler.addError("文件 [" + codeItem.getFileName() + "] 无法打开编辑器");
                                    // 继续处理下一个文件
                                    processFileModification(instruction, contextList, currentIndex + 1, totalFiles,
                                            successCount);
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

                                // 处理完当前文件后，继续下一个
                                processFileModification(instruction, contextList, currentIndex + 1, totalFiles,
                                        successCount);

                            } catch (Exception ex) {
                                System.err.println("处理文件 [" + codeItem.getFileName() + "] 修改失败: " + ex.getMessage());
                                ex.printStackTrace();
                                responseHandler
                                        .addError("处理文件 [" + codeItem.getFileName() + "] 修改失败: " + ex.getMessage());
                                // 继续处理下一个文件
                                processFileModification(instruction, contextList, currentIndex + 1, totalFiles,
                                        successCount);
                            }
                        });
                    }
                },
                // onError
                error -> {
                    responseHandler.addError("文件 [" + codeItem.getFileName() + "] 修改失败: " + error);
                    // 继续处理下一个文件
                    processFileModification(instruction, contextList, currentIndex + 1, totalFiles, successCount);
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
        String fullMessage = buildFullMessage(message, contextList);

        // 调用AI服务（传递项目路径，让AI能自主读取代码）
        String projectPath = project.getBasePath();
        aiClient.sendMessage(
                fullMessage,
                projectPath,
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
                    queueManager.completeCurrentRequest();
                    inputPanel.requestInputFocus();
                },
                // onError
                error -> {
                    thinkingManager.hide();
                    responseHandler.addError(error);
                    queueManager.failCurrentRequest();
                    inputPanel.requestInputFocus();
                });
    }

    /**
     * 构建包含上下文的完整消息
     */
    private String buildFullMessage(String userMessage, java.util.List<ContextService.ContextItem> contextList) {
        if (contextList != null && !contextList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ContextService.ContextItem item : contextList) {
                sb.append("文件: ").append(item.getFileName()).append("\n");
                sb.append("代码:\n").append(item.getContent()).append("\n\n");
            }
            sb.append("用户问题:\n").append(userMessage);
            return sb.toString();
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
