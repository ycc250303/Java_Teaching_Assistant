package com.example.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.*;

public class ChatToolWindowContent {
    private final JPanel mainPanel;
    private final Project project;

    public ChatToolWindowContent(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());

        // 创建聊天显示区域
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true); // 启用自动换行
        chatArea.setWrapStyleWord(false); // 在任意字符处换行（不按单词）
        chatArea.setText("欢迎使用智能会话助手！\n");
        JBScrollPane scrollPane = new JBScrollPane(chatArea); // 添加滚动条

        // 创建输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextArea inputField = new JTextArea(3, 20);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        JBScrollPane inputScrollPane = new JBScrollPane(inputField);
        JButton sendButton = new JButton("发送");

        // 使用 InputMap 和 ActionMap 处理键盘快捷键
        InputMap inputMap = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = inputField.getActionMap();

        // Enter 键：发送消息
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        actionMap.put("send", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendButton.doClick();
            }
        });

        // Shift+Enter：换行（保持默认行为）
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");

        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // 发送按钮事件
        sendButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                chatArea.append("用户: " + message + "\n");
                inputField.setText("");
                inputField.requestFocus();
                // TODO: 在这里添加AI响应逻辑
                chatArea.append("AI: 收到您的消息\n");
            }
        });

        // 组装界面
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
    }

    public JComponent getContent() {
        return mainPanel;
    }
}
