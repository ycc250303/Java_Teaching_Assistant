package com.javaProgram.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.javaProgram.ui.ChatToolWindowContent;
import com.javaProgram.ui.ChatToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class AskQuestionAboutCodeAction extends AnAction {

    public AskQuestionAboutCodeAction() {
        super("询问代码相关问题");
    }

    public AskQuestionAboutCodeAction(@NotNull String text) {
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
            Messages.showInfoMessage(project, "请先选中要询问的代码", "提示");
            return;
        }

        // 构建问题消息
        String fileName = psiFile.getName();
        String question = String.format("请帮我解释以下代码：\n\n文件: %s\n\n代码:\n```\n%s\n```", fileName, selectedText);

        // 获取聊天窗口内容并发送消息
        ChatToolWindowContent chatContent = ChatToolWindowFactory.getChatContent(project);

        if (chatContent != null) {
            // 打开工具窗口
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("智能会话");
            if (toolWindow != null) {
                toolWindow.show(() -> {
                    // 在工具窗口显示后发送消息
                    chatContent.sendMessage(question);
                });
            } else {
                // 如果工具窗口不存在，直接发送消息（工具窗口可能尚未初始化）
                chatContent.sendMessage(question);
            }
        } else {
            // 聊天窗口尚未初始化，提示用户
            Messages.showInfoMessage(
                    project,
                    "请先打开'智能会话'工具窗口（View -> Tool Windows -> 智能会话）",
                    "提示"
            );
        }
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