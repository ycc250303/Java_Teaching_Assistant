package com.example.ui;

import com.example.services.AiServiceClient;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.*;

public class ChatToolWindowContent {
    private final JPanel mainPanel;
    private final Project project;
    private final AiServiceClient aiClient; // 新增：AI 客户端

    public ChatToolWindowContent(Project project) {
        this.project = project;

        this.mainPanel = new JPanel(new BorderLayout());
        // 初始化 AI 客户端，每个项目使用唯一的 memoryId
        this.aiClient = new AiServiceClient(project.hashCode());

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
                inputField.setEnabled(false); // 发送时禁用输入框
                sendButton.setEnabled(false); // 禁用发送按钮

                chatArea.append("AI: ");

                int aiResponseStart = chatArea.getText().length(); // 记录 AI 响应开始位置

                // 调用 AI 服务
                aiClient.sendMessage(
                        message,
                        // onChunk: 接收到数据块
                        chunk -> {
                            chatArea.append(chunk);
                            // 自动滚动到底部
                            chatArea.setCaretPosition(chatArea.getText().length());
                        },
                        // onComplete: 完成
                        () -> {
                            chatArea.append("\n");
                            inputField.setEnabled(true); // 恢复输入框
                            sendButton.setEnabled(true); // 恢复发送按钮
                            inputField.requestFocus();
                        },
                        // onError: 出错
                        error -> {
                            chatArea.append("\n[错误] " + error + "\n");
                            inputField.setEnabled(true);
                            sendButton.setEnabled(true);
                            inputField.requestFocus();
                        });
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
