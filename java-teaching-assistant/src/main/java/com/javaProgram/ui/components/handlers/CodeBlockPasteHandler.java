package com.javaProgram.ui.components.handlers;

import com.intellij.openapi.project.Project;
import com.javaProgram.services.ContextService;
import com.javaProgram.utils.CodeBlockParser;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.function.Consumer;

/**
 * 代码块粘贴处理器
 * 处理粘贴的代码块并自动添加到上下文
 */
public class CodeBlockPasteHandler {
    private final Project project;
    private final ContextService contextService;
    private final Consumer<String> onErrorHint; // 显示错误提示的回调
    private final Runnable onContextAdded; // 上下文添加回调

    public CodeBlockPasteHandler(Project project, ContextService contextService,
            Consumer<String> onErrorHint, Runnable onContextAdded) {
        this.project = project;
        this.contextService = contextService;
        this.onErrorHint = onErrorHint;
        this.onContextAdded = onContextAdded;
    }

    /**
     * 处理粘贴事件
     *
     * @return 是否识别为代码块并已处理
     */
    public boolean handlePaste() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String pastedText = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                return handlePastedText(pastedText);
            }
        } catch (Exception ex) {
            // 出错时返回false，让系统处理默认粘贴
        }
        return false;
    }

    /**
     * 处理粘贴的文本
     *
     * @param pastedText 粘贴的文本
     * @return 是否识别为代码块并已处理
     */
    public boolean handlePastedText(String pastedText) {
        if (pastedText == null || pastedText.trim().isEmpty()) {
            return false;
        }

        CodeBlockParser.CodeBlock codeBlock = CodeBlockParser.parse(pastedText, project);
        if (codeBlock != null && codeBlock.isValid()) {
            handleCodeBlock(codeBlock);
            return true;
        }

        return false;
    }

    /**
     * 处理代码块
     */
    private void handleCodeBlock(CodeBlockParser.CodeBlock codeBlock) {
        if (contextService == null) {
            showErrorHint("上下文服务不可用");
            return;
        }

        try {
            ContextService.ContextItem contextItem = createContextItem(codeBlock);
            contextService.addContext(contextItem);
            notifyContextAdded();

        } catch (Exception e) {
            showErrorHint("添加代码块到上下文失败: " + e.getMessage());
        }
    }

    /**
     * 从 CodeBlock 创建 ContextItem
     */
    private ContextService.ContextItem createContextItem(CodeBlockParser.CodeBlock codeBlock) {
        return new ContextService.ContextItem(
                codeBlock.fileName != null ? codeBlock.fileName : "code_snippet",
                codeBlock.filePath != null ? codeBlock.filePath : "",
                codeBlock.startLine,
                codeBlock.endLine,
                codeBlock.code);
    }

    /**
     * 通知上下文已添加
     */
    private void notifyContextAdded() {
        if (onContextAdded != null) {
            SwingUtilities.invokeLater(onContextAdded);
        }
    }

    /**
     * 显示错误提示信息
     */
    private void showErrorHint(String message) {
        if (onErrorHint != null) {
            onErrorHint.accept(message);
        }
    }
}
