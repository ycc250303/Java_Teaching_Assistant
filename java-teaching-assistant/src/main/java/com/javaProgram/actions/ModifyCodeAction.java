package com.javaProgram.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.javaProgram.services.AiServiceClient;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 代码修改Action
 * 允许用户选中代码后输入修改指令，由AI修改代码并应用到编辑器
 */
public class ModifyCodeAction extends AnAction {

    public ModifyCodeAction() {
        super("AI修改代码");
    }

    public ModifyCodeAction(@NotNull String text) {
        super(text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前项目和编辑器
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (editor == null || psiFile == null || project == null) {
            return;
        }

        // 获取选中的文本
        String selectedText = editor.getSelectionModel().getSelectedText();

        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showInfoMessage(project, "请先选中要修改的代码", "提示");
            return;
        }

        // 使用自定义对话框获取多行输入
        String instruction = showMultilineInputDialog(project, "请输入修改指令：\n\n例如：添加注释、优化代码、修复bug等", "AI代码修改");

        // 用户取消了输入或输入为空
        if (instruction == null || instruction.trim().isEmpty()) {
            return;
        }

        // 获取文件名
        String fileName = psiFile.getName();

        // 创建AI服务客户端（使用项目hashCode作为memoryId）
        AiServiceClient aiClient = new AiServiceClient(project.hashCode());

        // 显示处理中的消息
        Messages.showInfoMessage(
                project,
                "正在请求AI修改代码，请稍候...",
                "处理中"
        );

        // 调用AI服务修改代码
        aiClient.modifyCode(
                selectedText,
                instruction.trim(),
                fileName,
                // onSuccess: 成功回调，将修改后的代码应用到编辑器
                modifiedCode -> {
                    // 在UI线程中执行文档修改
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            // 使用WriteCommandAction来修改文档（这是IntelliJ Platform的要求）
                            WriteCommandAction.runWriteCommandAction(project, () -> {
                                Document document = editor.getDocument();

                                // 获取选中文本的起始和结束位置
                                int startOffset = editor.getSelectionModel().getSelectionStart();
                                int endOffset = editor.getSelectionModel().getSelectionEnd();

                                // 清理代码：去除可能的markdown代码块标记
                                String cleanedCode = cleanCode(modifiedCode);

                                // 替换选中的文本为修改后的代码
                                document.replaceString(startOffset, endOffset, cleanedCode);

                                // 清除选中状态
                                editor.getSelectionModel().removeSelection();
                            });

                            // 显示成功消息
                            Messages.showInfoMessage(
                                    project,
                                    "代码修改成功并已应用到编辑器！",
                                    "修改成功"
                            );
                        } catch (Exception ex) {
                            // 显示错误消息
                            Messages.showErrorDialog(
                                    project,
                                    "应用代码修改时出错: " + ex.getMessage(),
                                    "错误"
                            );
                        }
                    });
                },
                // onError: 失败回调
                error -> {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "AI代码修改失败: " + error,
                                "修改失败"
                        );
                    });
                }
        );
    }

    /**
     * 显示多行输入对话框
     */
    private String showMultilineInputDialog(Project project, String message, String title) {
        // 创建对话框包装器
        MultilineInputDialog dialog = new MultilineInputDialog(project, message, title);
        dialog.show();

        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return dialog.getText();
        }

        return null;
    }

    /**
     * 多行输入对话框类
     */
    private static class MultilineInputDialog extends DialogWrapper {
        private JTextArea textArea;
        private final String message;

        protected MultilineInputDialog(Project project, String message, String title) {
            super(project, true);
            this.message = message;
            setTitle(title);
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            // 创建主面板
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // 添加说明标签
            JLabel label = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>");
            panel.add(label, BorderLayout.NORTH);

            // 创建多行文本区域
            textArea = new JTextArea(5, 30);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            // 创建滚动面板
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 120));
            panel.add(scrollPane, BorderLayout.CENTER);

            return panel;
        }

        public String getText() {
            return textArea != null ? textArea.getText() : "";
        }
    }

    /**
     * 清理代码：去除可能的markdown代码块标记
     * AI返回的代码可能被```java和```包裹，需要去掉
     */
    private String cleanCode(String code) {
        if (code == null) {
            return "";
        }

        String cleaned = code.trim();

        // 去除开头的```java、```或类似标记
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            } else {
                // 如果没有换行，可能是```code```的形式
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*", "");
            }
        }

        // 去除结尾的```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只有在有选中文本时才启用此Action
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();

        e.getPresentation().setEnabled(hasSelection);
        e.getPresentation().setVisible(hasSelection);
    }
}