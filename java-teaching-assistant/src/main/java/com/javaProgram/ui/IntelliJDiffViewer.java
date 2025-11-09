package com.javaProgram.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.javaProgram.services.CodeDiffResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * IntelliJ IDEA内置差异查看器
 * 使用IntelliJ的专业差异对比功能展示代码修改
 */
public class IntelliJDiffViewer {

    /**
     * 显示差异对比并等待用户确认
     *
     * @param project     当前项目
     * @param diffResult  差异结果
     * @param editor      编辑器
     * @param startOffset 选中文本起始位置
     * @param endOffset   选中文本结束位置
     * @return 差异查看器打开的文件，如果失败则返回null
     */
    @Nullable
    public static VirtualFile showDiffAndWaitForConfirmation(
            @NotNull Project project,
            @NotNull CodeDiffResult diffResult,
            @NotNull Editor editor,
            int startOffset,
            int endOffset) {

        try {
            // 检查是否有实际变化
            if (!diffResult.hasChanges()) {
                Messages.showInfoMessage(
                        project,
                        "AI建议的代码与原代码相同，无需修改。",
                        "无需修改");
                return null;
            }

            // 记录显示差异前打开的文件
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            Set<VirtualFile> openFilesBefore = new HashSet<>();
            for (VirtualFile file : editorManager.getOpenFiles()) {
                openFilesBefore.add(file);
            }

            // 创建差异内容
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();

            // 使用create方法创建简单内容
            DiffContent originalContent = contentFactory.create(
                    diffResult.getOriginalCode());
            DiffContent modifiedContent = contentFactory.create(
                    diffResult.getModifiedCode());

            // 创建差异请求
            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "AI代码修改预览 - " + (diffResult.getFileName() != null ? diffResult.getFileName() : ""),
                    originalContent,
                    modifiedContent,
                    "原始代码",
                    "修改后代码");

            // 使用IntelliJ的内置差异查看器显示差异
            DiffManager.getInstance().showDiff(project, diffRequest);

            // 找到新打开的文件（差异查看器）
            VirtualFile diffViewerFile = null;
            for (VirtualFile file : editorManager.getOpenFiles()) {
                if (!openFilesBefore.contains(file)) {
                    diffViewerFile = file;
                    break;
                }
            }

            return diffViewerFile;

        } catch (Exception e) {
            Messages.showErrorDialog(
                    project,
                    "显示差异对比时出错: " + e.getMessage(),
                    "错误");
            return null;
        }
    }

    /**
     * 显示差异对话框（从聊天界面调用）
     * 仅展示差异，不涉及应用修改
     *
     * @param project    当前项目
     * @param diffResult 差异结果
     */
    public static void showDiffDialog(
            @NotNull Project project,
            @NotNull CodeDiffResult diffResult) {

        try {
            // 检查是否有实际变化
            if (!diffResult.hasChanges()) {
                Messages.showInfoMessage(
                        project,
                        "代码没有变化。",
                        "无变化");
                return;
            }

            // 创建差异内容
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();

            DiffContent originalContent = contentFactory.create(
                    diffResult.getOriginalCode() != null ? diffResult.getOriginalCode() : "");
            DiffContent modifiedContent = contentFactory.create(
                    diffResult.getModifiedCode() != null ? diffResult.getModifiedCode() : "");

            // 创建差异请求
            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "AI代码修改预览 - " + (diffResult.getFileName() != null ? diffResult.getFileName() : "代码"),
                    originalContent,
                    modifiedContent,
                    "原始代码",
                    "修改后代码");

            // 使用IntelliJ的内置差异查看器显示差异
            DiffManager.getInstance().showDiff(project, diffRequest);

        } catch (Exception e) {
            Messages.showErrorDialog(
                    project,
                    "显示差异对比时出错: " + e.getMessage(),
                    "错误");
        }
    }

    /**
     * 应用修改到编辑器（从聊天界面调用）
     *
     * @param project     当前项目
     * @param diffResult  差异结果
     * @param editor      编辑器
     * @param startOffset 选中文本起始位置
     * @param endOffset   选中文本结束位置
     */
    public static void applyModification(
            @NotNull Project project,
            @NotNull CodeDiffResult diffResult,
            @NotNull Editor editor,
            int startOffset,
            int endOffset) {

        try {
            // 应用修改到编辑器
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Document document = editor.getDocument();
                document.replaceString(startOffset, endOffset, diffResult.getModifiedCode());
                editor.getSelectionModel().removeSelection();
            });

        } catch (Exception e) {
            Messages.showErrorDialog(
                    project,
                    "应用修改时出错: " + e.getMessage(),
                    "错误");
        }
    }
}