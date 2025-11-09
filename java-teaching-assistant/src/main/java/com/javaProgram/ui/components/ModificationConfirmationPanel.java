package com.javaProgram.ui.components;

import com.javaProgram.services.PendingModificationManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 修改确认面板
 * 显示代码修改确认界面（接受/拒绝）
 */
public class ModificationConfirmationPanel {
    private static final float SMALL_FONT_SIZE = 13f;
    private static final float MINI_FONT_SIZE = 12f;

    /**
     * 创建修改确认面板
     * 
     * @param modificationId 修改ID
     * @param fileName       修改的文件名
     * @return 确认面板
     */
    public static JPanel create(String modificationId, String fileName) {
        // 主面板 - 使用BoxLayout以确保不限制后续消息
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.Y_AXIS));
        wrapperPanel.setOpaque(false);
        wrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 内部消息面板 - 固定尺寸
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setAlignmentY(Component.TOP_ALIGNMENT);
        messagePanel.setMaximumSize(new Dimension(JBUI.scale(400), JBUI.scale(140)));
        messagePanel.setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(140)));
        messagePanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.BLUE, 1),
                JBUI.Borders.empty(8)));
        messagePanel.setBackground(JBUI.CurrentTheme.ToolWindow.background());

        // 消息内容面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        // 消息文本 - 显示文件名
        String messageContent = "代码修改已完成！\n文件: " + (fileName != null ? fileName : "未知") + "\n差异对比已在IntelliJ中显示。";
        JTextArea messageText = new JTextArea(messageContent);
        messageText.setEditable(false);
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setOpaque(false);
        messageText.setForeground(JBUI.CurrentTheme.Label.foreground());
        messageText.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, SMALL_FONT_SIZE));
        messageText.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
        messageText.setFocusable(true);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(JBUI.Borders.empty(0, 8, 8, 8));

        // 接受按钮
        JButton acceptButton = createAcceptButton(modificationId, wrapperPanel, fileName);

        // 拒绝按钮
        JButton rejectButton = createRejectButton(modificationId, wrapperPanel, acceptButton.getFont(), fileName);

        buttonPanel.add(acceptButton);
        buttonPanel.add(Box.createHorizontalStrut(JBUI.scale(10)));
        buttonPanel.add(rejectButton);

        // 组装面板
        contentPanel.add(messageText, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        messagePanel.add(contentPanel, BorderLayout.CENTER);

        // 保存按钮引用以便后续更新状态
        messagePanel.putClientProperty("acceptButton", acceptButton);
        messagePanel.putClientProperty("rejectButton", rejectButton);

        // 将内部面板包装到外部包装器中
        wrapperPanel.add(messagePanel);

        return wrapperPanel;
    }

    /**
     * 创建接受按钮
     */
    private static JButton createAcceptButton(String modificationId, JPanel wrapperPanel, String fileName) {
        JButton acceptButton = new JButton("✓ 接受修改");
        acceptButton.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        acceptButton.setForeground(JBColor.GREEN);
        acceptButton.setOpaque(false);
        acceptButton.setContentAreaFilled(false);
        acceptButton.setBorder(BorderFactory.createLineBorder(JBColor.GREEN, 1));
        acceptButton.setFocusPainted(false);
        acceptButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 保存原始尺寸和字体大小，用于恢复
        final Dimension originalSize = acceptButton.getPreferredSize();
        final Font originalFont = acceptButton.getFont();

        acceptButton.addActionListener(e -> {
            // 应用修改
            PendingModificationManager.applyModification(modificationId);
            // 更新消息为已接受状态（删除按钮）
            updateModificationStatus(wrapperPanel, "✅ 修改已成功应用到编辑器！", JBColor.GREEN, fileName);
        });

        acceptButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 只在按钮启用时响应悬停效果
                if (!acceptButton.isEnabled()) {
                    return;
                }
                // 颜色变深
                acceptButton.setForeground(new Color(0, 204, 102));
                acceptButton.setBorder(BorderFactory.createLineBorder(new Color(153, 255, 153), 1));
                // 按钮稍微放大（通过增加字体大小实现）
                Font largerFont = originalFont.deriveFont(Font.PLAIN, MINI_FONT_SIZE + 1);
                acceptButton.setFont(largerFont);
                // 重新计算并设置按钮大小
                acceptButton.setPreferredSize(new Dimension(
                        (int) (originalSize.width * 1.05),
                        (int) (originalSize.height * 1.1)));
                acceptButton.revalidate();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 只在按钮启用时恢复原始样式
                if (!acceptButton.isEnabled()) {
                    return;
                }
                // 恢复原始颜色
                acceptButton.setForeground(JBColor.GREEN);
                acceptButton.setBorder(BorderFactory.createLineBorder(JBColor.GREEN, 1));
                // 恢复原始大小和字体
                acceptButton.setFont(originalFont);
                acceptButton.setPreferredSize(originalSize);
                acceptButton.revalidate();
            }
        });

        return acceptButton;
    }

    /**
     * 创建拒绝按钮
     */
    private static JButton createRejectButton(String modificationId, JPanel wrapperPanel, Font originalFont,
            String fileName) {
        JButton rejectButton = new JButton("✗ 拒绝修改");
        rejectButton.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, MINI_FONT_SIZE));
        rejectButton.setForeground(JBColor.RED);
        rejectButton.setOpaque(false);
        rejectButton.setContentAreaFilled(false);
        rejectButton.setBorder(BorderFactory.createLineBorder(JBColor.RED, 1));
        rejectButton.setFocusPainted(false);
        rejectButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        final Dimension originalSize = rejectButton.getPreferredSize();

        rejectButton.addActionListener(e -> {
            // 拒绝修改
            PendingModificationManager.rejectModification(modificationId);
            // 更新消息为已拒绝状态（删除按钮）
            updateModificationStatus(wrapperPanel, "❌ 修改已取消", JBColor.RED, fileName);
        });

        rejectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 只在按钮启用时响应悬停效果
                if (!rejectButton.isEnabled()) {
                    return;
                }
                // 颜色变深
                rejectButton.setForeground(new Color(255, 51, 51));
                rejectButton.setBorder(BorderFactory.createLineBorder(new Color(204, 0, 0), 1));
                // 按钮稍微放大（通过增加字体大小实现）
                Font largerFont = originalFont.deriveFont(Font.PLAIN, MINI_FONT_SIZE + 1);
                rejectButton.setFont(largerFont);
                // 重新计算并设置按钮大小
                rejectButton.setPreferredSize(new Dimension(
                        (int) (originalSize.width * 1.05),
                        (int) (originalSize.height * 1.1)));
                rejectButton.revalidate();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 只在按钮启用时恢复原始样式
                if (!rejectButton.isEnabled()) {
                    return;
                }
                // 恢复原始颜色
                rejectButton.setForeground(JBColor.RED);
                rejectButton.setBorder(BorderFactory.createLineBorder(JBColor.RED, 1));
                // 恢复原始大小和字体
                rejectButton.setFont(originalFont);
                rejectButton.setPreferredSize(originalSize);
                rejectButton.revalidate();
            }
        });

        return rejectButton;
    }

    /**
     * 更新修改状态 - 删除按钮并显示结果
     */
    private static void updateModificationStatus(JPanel wrapperPanel, String statusText, Color statusColor,
            String fileName) {
        // 获取内部消息面板
        if (wrapperPanel.getComponentCount() > 0) {
            Component firstComponent = wrapperPanel.getComponent(0);
            if (firstComponent instanceof JPanel) {
                JPanel messagePanel = (JPanel) firstComponent;

                // 找到内容面板并删除按钮面板
                Component[] components = messagePanel.getComponents();
                for (Component component : components) {
                    if (component instanceof JPanel) {
                        JPanel contentPanel = (JPanel) component;
                        Component[] contentComponents = contentPanel.getComponents();

                        // 遍历内容面板的组件
                        for (int i = 0; i < contentComponents.length; i++) {
                            Component contentComponent = contentComponents[i];

                            // 找到按钮面板（位于SOUTH位置），直接删除
                            if (contentComponent instanceof JPanel) {
                                JPanel buttonPanel = (JPanel) contentComponent;
                                // 检查是否为按钮面板（包含按钮）
                                boolean isButtonPanel = false;
                                for (Component child : buttonPanel.getComponents()) {
                                    if (child instanceof JButton) {
                                        isButtonPanel = true;
                                        break;
                                    }
                                }

                                if (isButtonPanel) {
                                    // 创建状态标签
                                    JLabel statusLabel = new JLabel(statusText);
                                    statusLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, SMALL_FONT_SIZE));
                                    statusLabel.setForeground(statusColor);
                                    statusLabel.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
                                    statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                                    // 删除按钮面板，添加状态标签
                                    contentPanel.remove(buttonPanel);
                                    contentPanel.add(statusLabel, BorderLayout.SOUTH);
                                    break;
                                }
                            }
                        }

                        // 更新主消息文本 - 保留文件名信息
                        for (Component contentComponent : contentComponents) {
                            if (contentComponent instanceof JTextArea) {
                                JTextArea textArea = (JTextArea) contentComponent;
                                String displayFileName = (fileName != null ? fileName : "未知");
                                textArea.setText("AI代码修改：\n文件: " + displayFileName);
                                textArea.setForeground(JBUI.CurrentTheme.Label.foreground());
                                break;
                            }
                        }
                        break;
                    }
                }

                // 更新边框颜色以反映状态
                messagePanel.setBorder(JBUI.Borders.compound(
                        JBUI.Borders.customLine(statusColor, 1),
                        JBUI.Borders.empty(8)));

                // 调整面板高度以适应新内容（按钮删除后高度减小）
                messagePanel.setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(90)));
                messagePanel.setMaximumSize(new Dimension(JBUI.scale(400), JBUI.scale(90)));

                // 刷新显示
                wrapperPanel.revalidate();
                wrapperPanel.repaint();
            }
        }
    }
}
