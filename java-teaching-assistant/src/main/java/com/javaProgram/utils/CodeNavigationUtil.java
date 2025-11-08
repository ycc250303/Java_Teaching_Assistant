package com.javaProgram.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.javaProgram.services.ContextService;

import javax.swing.*;
import java.awt.*;

/**
 * 代码导航工具类
 * 提供跳转到代码位置的通用功能
 */
public class CodeNavigationUtil {

    /**
     * 导航到代码位置
     * 
     * @param project         当前项目
     * @param item            上下文项（包含文件路径和行号信息）
     * @param parentComponent 父组件（用于显示错误对话框）
     */
    public static void navigateToCode(Project project, ContextService.ContextItem item, Component parentComponent) {
        if (project == null || item == null || item.getFilePath() == null || item.getFilePath().isEmpty()) {
            return;
        }

        // 在后台线程执行文件系统操作（需要 read action）
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 在 read action 中访问文件系统
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    // 获取虚拟文件（需要 read action）
                    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(item.getFilePath());

                    if (virtualFile != null && virtualFile.exists()) {
                        // 计算跳转的行号（从0开始）
                        int line = Math.max(0, item.getStartLine() - 1);

                        // 在 EDT 线程中打开编辑器（UI 操作必须在 EDT）
                        SwingUtilities.invokeLater(() -> {
                            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile, line, 0);
                            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                        });
                    } else {
                        // 文件不存在，在 EDT 线程中显示提示
                        SwingUtilities.invokeLater(() -> {
                            if (parentComponent != null) {
                                JOptionPane.showMessageDialog(
                                        parentComponent,
                                        "无法找到文件: " + item.getFilePath(),
                                        "文件未找到",
                                        JOptionPane.WARNING_MESSAGE);
                            }
                        });
                    }
                } catch (Exception e) {
                    // 处理异常，在 EDT 线程中显示
                    SwingUtilities.invokeLater(() -> {
                        if (parentComponent != null) {
                            JOptionPane.showMessageDialog(
                                    parentComponent,
                                    "跳转失败: " + e.getMessage(),
                                    "错误",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });
        });
    }

    /**
     * 为标签添加可点击效果（悬停变色 + 手型光标）
     * 
     * @param label        要添加效果的标签
     * @param defaultColor 默认颜色
     * @param hoverColor   悬停颜色
     */
    public static void addClickableEffect(JLabel label, Color defaultColor, Color hoverColor) {
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                label.setForeground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                label.setForeground(defaultColor);
            }
        });
    }
}
