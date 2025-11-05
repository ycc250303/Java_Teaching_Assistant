package com.javaProgram.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.javaProgram.services.ContextService;
import org.jetbrains.annotations.NotNull;

public class AddToContextAction extends AnAction {

    public AddToContextAction() {
        super("添加到AI上下文");
    }

    public AddToContextAction(@NotNull String text) {
        super(text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前项目和编辑器
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (editor == null || psiFile == null) {
            return;
        }

        // 获取选中的文本
        String selectedText = editor.getSelectionModel().getSelectedText();

        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showInfoMessage(project, "请先选中要添加到上下文的代码", "提示");
            return;
        }

        // 获取选中文本的起始和结束位置
        int startOffset = editor.getSelectionModel().getSelectionStart();
        int endOffset = editor.getSelectionModel().getSelectionEnd();
        
        // 获取对应的行号（从0开始，所以需要+1）
        int startLine = editor.getDocument().getLineNumber(startOffset) + 1;
        int endLine = editor.getDocument().getLineNumber(endOffset) + 1;

        // 获取上下文服务并添加代码
        ContextService contextService = ServiceManager.getService(project, ContextService.class);
        if (contextService != null) {
            String fileName = psiFile.getName();
            String filePath = psiFile.getVirtualFile().getPath();
            
            // 创建结构化的上下文项
            ContextService.ContextItem contextItem = new ContextService.ContextItem(
                fileName,
                filePath,
                startLine,
                endLine,
                selectedText
            );
            
            contextService.addContext(contextItem);

            // 显示成功消息
            String lineInfo = startLine == endLine ? 
                "行号: " + startLine : 
                "行号: " + startLine + "-" + endLine;
            
            Messages.showInfoMessage(
                project,
                "已成功添加到AI上下文！\n\n文件: " + fileName + "\n" + lineInfo + "\n代码长度: " + selectedText.length() + " 字符",
                "添加成功"
            );
        } else {
            Messages.showErrorDialog(project, "无法获取上下文服务", "错误");
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