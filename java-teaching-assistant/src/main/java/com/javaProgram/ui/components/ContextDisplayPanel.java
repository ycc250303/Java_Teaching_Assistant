package com.javaProgram.ui.components;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.javaProgram.services.ContextService;
import com.javaProgram.ui.layout.WrapLayout;

import javax.swing.*;
import java.awt.*;

/**
 * 上下文显示面板
 * 用于显示用户添加的代码上下文卡片
 */
public class ContextDisplayPanel extends JPanel {
    private static final float MINI_FONT_SIZE = 12f;
    private final ContextService contextService;
    private final Project project;

    public ContextDisplayPanel(ContextService contextService, Project project) {
        this.contextService = contextService;
        this.project = project;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(JBColor.PanelBackground);
        setBorder(JBUI.Borders.empty(4, 8));
        setVisible(false); // 初始隐藏
    }

    /**
     * 更新上下文显示面板，类似Cursor的样式
     */
    public void updateContextDisplay(java.util.List<ContextService.ContextItem> contextList) {
        // 清空面板
        removeAll();

        if (contextList.isEmpty()) {
            setVisible(false);
        } else {
            setVisible(true);

            // 获取背景色
            Color ideBackgroundColor = JBColor.PanelBackground;
            Color chipBackgroundColor = lightenColor(ideBackgroundColor, 0.1f);
            Color chipBorderColor = lightenColor(ideBackgroundColor, 0.2f);

            // 使用支持自动换行的WrapLayout
            JPanel chipsContainer = new JPanel(new WrapLayout(WrapLayout.LEFT, JBUI.scale(4), JBUI.scale(4)));
            chipsContainer.setBackground(getBackground());

            // 为每个上下文项创建一个芯片
            for (int i = 0; i < contextList.size(); i++) {
                ContextService.ContextItem item = contextList.get(i);
                JPanel chip = createContextChip(item, chipBackgroundColor, chipBorderColor);
                chipsContainer.add(chip);
            }

            // 使用 JScrollPane 包装容器，以支持滚动
            JBScrollPane scrollPane = new JBScrollPane(chipsContainer);
            scrollPane.setBackground(getBackground());
            scrollPane.getViewport().setBackground(getBackground());
            scrollPane.setBorder(JBUI.Borders.empty());
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            // 动态计算需要的高度
            int chipHeight = JBUI.scale(32);
            int rows = (int) Math.ceil((double) contextList.size() / 3);
            int estimatedHeight = Math.min(rows * chipHeight + JBUI.scale(20), JBUI.scale(150));

            scrollPane.setPreferredSize(new Dimension(Integer.MAX_VALUE, estimatedHeight));
            scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(150)));

            add(scrollPane);
        }

        // 刷新布局
        revalidate();
        repaint();
    }

    /**
     * 创建单个上下文芯片，类似Cursor的样式
     */
    private JPanel createContextChip(ContextService.ContextItem item, Color backgroundColor, Color borderColor) {
        JPanel chip = new JPanel(new BorderLayout());
        chip.setBackground(backgroundColor);
        chip.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(borderColor, 1),
                JBUI.Borders.empty(4, 8)));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 创建文本标签：文件名 + 行数
        String displayText = item.getFileName();
        if (item.getStartLine() > 0) {
            displayText += " (" + item.getLineRangeText() + ")";
        }

        JLabel textLabel = new JLabel(displayText);
        textLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        textLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
        textLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 为文本标签添加点击事件 - 跳转到代码位置
        textLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                navigateToCode(item);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                // 鼠标悬停时文字变色
                textLabel.setForeground(new JBColor(new Color(0, 120, 215), new Color(100, 149, 237)));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // 鼠标离开恢复原色
                textLabel.setForeground(JBUI.CurrentTheme.Label.foreground());
            }
        });

        // 创建删除按钮
        JLabel closeLabel = new JLabel("✕");
        closeLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        closeLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeLabel.setBorder(JBUI.Borders.emptyLeft(6));

        // 删除按钮鼠标悬停效果
        closeLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeLabel.setForeground(JBColor.RED);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // 删除该上下文项 - 使用当前最新的列表重新查找索引
                if (contextService != null) {
                    java.util.List<ContextService.ContextItem> currentList = contextService.getContextList();
                    for (int i = 0; i < currentList.size(); i++) {
                        ContextService.ContextItem current = currentList.get(i);
                        if (current.getFileName().equals(item.getFileName()) &&
                                current.getFilePath().equals(item.getFilePath()) &&
                                current.getStartLine() == item.getStartLine() &&
                                current.getEndLine() == item.getEndLine()) {
                            contextService.removeContext(i);
                            break;
                        }
                    }
                }
            }
        });

        // 组装芯片
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(textLabel);
        contentPanel.add(closeLabel);

        chip.add(contentPanel, BorderLayout.CENTER);

        return chip;
    }

    /**
     * 导航到代码位置
     */
    private void navigateToCode(ContextService.ContextItem item) {
        if (project == null || item.getFilePath() == null || item.getFilePath().isEmpty()) {
            return;
        }

        // 在EDT线程外执行文件操作
        SwingUtilities.invokeLater(() -> {
            try {
                // 获取虚拟文件
                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(item.getFilePath());

                if (virtualFile != null && virtualFile.exists()) {
                    // 计算跳转的行号（从0开始）
                    int line = Math.max(0, item.getStartLine() - 1);

                    // 打开文件并跳转到指定行
                    OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile, line, 0);
                    FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                } else {
                    // 文件不存在，显示提示
                    JOptionPane.showMessageDialog(
                            this,
                            "无法找到文件: " + item.getFilePath(),
                            "文件未找到",
                            JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                // 处理异常
                JOptionPane.showMessageDialog(
                        this,
                        "打开文件时出错: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
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
}
