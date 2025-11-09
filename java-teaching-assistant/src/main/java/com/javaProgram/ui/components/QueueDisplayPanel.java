package com.javaProgram.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.javaProgram.services.RequestQueueManager;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.util.List;

/**
 * 请求队列显示面板
 * 职责：可视化显示当前处理中和等待中的AI请求
 */
public class QueueDisplayPanel extends JPanel {
    private final JLabel titleLabel;
    private final JPanel queueContainer;
    private final RequestQueueManager queueManager;

    public QueueDisplayPanel(RequestQueueManager queueManager) {
        this.queueManager = queueManager;

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(4, 8));
        setVisible(false); // 默认隐藏，有队列时才显示

        // 标题标签
        titleLabel = new JLabel("📋 请求队列 (0/3)");
        titleLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, 12f));
        titleLabel.setBorder(JBUI.Borders.empty(4, 0));

        // 队列容器
        queueContainer = new JPanel();
        queueContainer.setLayout(new BoxLayout(queueContainer, BoxLayout.Y_AXIS));
        queueContainer.setBorder(JBUI.Borders.empty(4, 0));

        // 组装UI
        add(titleLabel, BorderLayout.NORTH);
        add(queueContainer, BorderLayout.CENTER);

        // 监听队列变化
        queueManager.addListener(this::updateQueueDisplay);
    }

    /**
     * 更新队列显示
     */
    private void updateQueueDisplay(List<RequestQueueManager.QueuedRequest> queue,
            RequestQueueManager.QueuedRequest current) {
        SwingUtilities.invokeLater(() -> {
            queueContainer.removeAll();

            // 显示当前正在处理的请求
            if (current != null) {
                queueContainer.add(createQueueItemPanel(current, true));
                queueContainer.add(Box.createVerticalStrut(4)); // 间距
            }

            // 显示等待中的请求
            for (int i = 0; i < queue.size(); i++) {
                queueContainer.add(createQueueItemPanel(queue.get(i), false));
                if (i < queue.size() - 1) {
                    queueContainer.add(Box.createVerticalStrut(4)); // 间距
                }
            }

            // 更新标题
            int totalCount = queue.size() + (current != null ? 1 : 0);
            titleLabel.setText("📋 请求队列 (" + queue.size() + "/3)");

            // 如果队列为空且没有当前请求，隐藏面板
            if (totalCount == 0) {
                setVisible(false);
            } else {
                setVisible(true);
            }

            revalidate();
            repaint();
        });
    }

    /**
     * 创建单个队列项的面板
     */
    private JPanel createQueueItemPanel(RequestQueueManager.QueuedRequest request, boolean isCurrent) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(JBUI.Borders.empty(6, 10));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // 状态图标和文本
        String icon;
        String statusText;
        Color borderColor;
        Color backgroundColor;

        if (isCurrent) {
            icon = "⚙️";
            statusText = "处理中";
            borderColor = new JBColor(new Color(0, 120, 215), new Color(100, 149, 237));
            backgroundColor = new JBColor(new Color(230, 242, 255), new Color(45, 55, 72));
        } else {
            icon = "⏳";
            statusText = "等待中";
            borderColor = JBColor.GRAY;
            backgroundColor = new JBColor(new Color(245, 245, 245), new Color(55, 55, 55));
        }

        panel.setBackground(backgroundColor);
        panel.setBorder(new RoundedBorder(borderColor, isCurrent ? 2 : 1, 8));

        // 状态图标标签
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, 14f));

        // 消息预览
        String preview = truncate(request.getMessage(), 35);
        JLabel messageLabel = new JLabel(preview);
        messageLabel.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, 11f));
        messageLabel.setToolTipText(request.getMessage()); // 完整消息作为tooltip

        // 状态标签
        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, 10f));
        statusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());

        // 左侧面板（图标 + 消息）
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(iconLabel);
        leftPanel.add(messageLabel);

        // 组装
        panel.add(leftPanel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 圆角边框
     */
    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                    width - thickness - 1, height - thickness - 1,
                    radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 2, thickness + 2, thickness + 2, thickness + 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = thickness + 2;
            return insets;
        }
    }
}
