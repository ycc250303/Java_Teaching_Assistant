package com.javaProgram.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.javaProgram.services.CodeDiffResult;
import org.jetbrains.annotations.NotNull;

/**
 * IntelliJ IDEA内置差异查看器
 * 使用IntelliJ的专业差异对比功能展示代码修改
 */
public class IntelliJDiffViewer {

    /**
     * 显示差异对比并等待用户确认
     *
     * @param project      当前项目
     * @param diffResult   差异结果
     * @param editor       编辑器
     * @param startOffset  选中文本起始位置
     * @param endOffset    选中文本结束位置
     * @return 用户是否接受了修改
     */
    public static boolean showDiffAndWaitForConfirmation(
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
                        "无需修改"
                );
                return false;
            }

            // 创建差异内容
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();

            // 使用create方法创建简单内容
            DiffContent originalContent = contentFactory.create(
                    diffResult.getOriginalCode()
            );
            DiffContent modifiedContent = contentFactory.create(
                    diffResult.getModifiedCode()
            );

            // 创建差异请求
            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "AI代码修改预览 - " + (diffResult.getFileName() != null ? diffResult.getFileName() : ""),
                    originalContent,
                    modifiedContent,
                    "原始代码",
                    "修改后代码"
            );

            // 使用IntelliJ的内置差异查看器显示差异
            DiffManager.getInstance().showDiff(project, diffRequest);

            // 直接返回true，让调用者知道差异显示器已显示
            // 实际的确认逻辑将在聊天界面中处理
            return true;

        } catch (Exception e) {
            Messages.showErrorDialog(
                    project,
                    "显示差异对比时出错: " + e.getMessage(),
                    "错误"
            );
            return false;
        }
    }

    /**
     * 应用修改到编辑器（从聊天界面调用）
     *
     * @param project      当前项目
     * @param diffResult   差异结果
     * @param editor       编辑器
     * @param startOffset  选中文本起始位置
     * @param endOffset    选中文本结束位置
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

            Messages.showInfoMessage(
                    project,
                    "代码修改成功并已应用到编辑器！",
                    "修改成功"
            );

        } catch (Exception e) {
            Messages.showErrorDialog(
                    project,
                    "应用修改时出错: " + e.getMessage(),
                    "错误"
            );
        }
    }
}