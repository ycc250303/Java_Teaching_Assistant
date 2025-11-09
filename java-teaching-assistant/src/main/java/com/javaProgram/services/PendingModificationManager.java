package com.javaProgram.services;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 待确认代码修改管理器
 * 用于存储和管理用户待确认的代码修改
 */
public class PendingModificationManager {
    private static final Map<String, PendingModification> pendingModifications = new HashMap<>();

    /**
     * 添加待确认的修改
     *
     * @param project        项目
     * @param editor         编辑器
     * @param diffResult     差异结果
     * @param startOffset    起始位置
     * @param endOffset      结束位置
     * @param diffViewerFile 差异查看器打开的文件
     * @return 修改ID
     */
    public static String addPendingModification(Project project, Editor editor, CodeDiffResult diffResult,
            int startOffset, int endOffset, com.intellij.openapi.vfs.VirtualFile diffViewerFile) {
        String modificationId = UUID.randomUUID().toString();
        PendingModification modification = new PendingModification(
                project, editor, diffResult, startOffset, endOffset, diffViewerFile);
        pendingModifications.put(modificationId, modification);
        return modificationId;
    }

    /**
     * 获取待确认的修改
     *
     * @param modificationId 修改ID
     * @return 待确认的修改
     */
    public static PendingModification getPendingModification(String modificationId) {
        return pendingModifications.get(modificationId);
    }

    /**
     * 应用修改
     *
     * @param modificationId 修改ID
     */
    public static void applyModification(String modificationId) {
        PendingModification modification = pendingModifications.get(modificationId);
        if (modification != null) {
            // 关闭差异查看器
            closeDiffViewer(modification);

            // 应用修改
            com.javaProgram.ui.IntelliJDiffViewer.applyModification(
                    modification.getProject(),
                    modification.getDiffResult(),
                    modification.getEditor(),
                    modification.getStartOffset(),
                    modification.getEndOffset());

            // 移除已处理的修改
            pendingModifications.remove(modificationId);
        }
    }

    /**
     * 拒绝修改
     *
     * @param modificationId 修改ID
     */
    public static void rejectModification(String modificationId) {
        PendingModification modification = pendingModifications.get(modificationId);
        if (modification != null) {
            // 关闭差异查看器
            closeDiffViewer(modification);

            // 移除已处理的修改
            pendingModifications.remove(modificationId);
        }
    }

    /**
     * 关闭差异查看器
     */
    private static void closeDiffViewer(PendingModification modification) {
        try {
            com.intellij.openapi.vfs.VirtualFile diffViewerFile = modification.getDiffViewerFile();
            if (diffViewerFile != null) {
                Project project = modification.getProject();
                FileEditorManager editorManager = FileEditorManager.getInstance(project);
                editorManager.closeFile(diffViewerFile);
            }
        } catch (Exception e) {
            // 忽略关闭差异查看器时的异常，不影响主流程
            System.err.println("关闭差异查看器时出错: " + e.getMessage());
        }
    }

    /**
     * 获取所有待确认的修改
     *
     * @return 待确认修改的Map
     */
    public static Map<String, PendingModification> getAllPendingModifications() {
        return new HashMap<>(pendingModifications);
    }

    /**
     * 待确认的修改数据类
     */
    public static class PendingModification {
        private final Project project;
        private final Editor editor;
        private final CodeDiffResult diffResult;
        private final int startOffset;
        private final int endOffset;
        private final com.intellij.openapi.vfs.VirtualFile diffViewerFile;

        public PendingModification(Project project, Editor editor, CodeDiffResult diffResult,
                int startOffset, int endOffset, com.intellij.openapi.vfs.VirtualFile diffViewerFile) {
            this.project = project;
            this.editor = editor;
            this.diffResult = diffResult;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.diffViewerFile = diffViewerFile;
        }

        // Getters
        public Project getProject() {
            return project;
        }

        public Editor getEditor() {
            return editor;
        }

        public CodeDiffResult getDiffResult() {
            return diffResult;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public com.intellij.openapi.vfs.VirtualFile getDiffViewerFile() {
            return diffViewerFile;
        }
    }
}