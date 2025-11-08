package com.javaProgram.ui.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.JBColor;
import com.javaProgram.services.ContextService;
import com.javaProgram.utils.CodeBlockParser;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.TransferHandler;

/**
 * 聊天输入面板
 * 包含输入框、发送按钮及自动高度调整
 */
public class ChatInputPanel extends JPanel {
    private final JTextArea inputField;
    private final JButton sendButton;
    private final JBScrollPane inputScrollPane;
    private final int defaultHeight;
    private final Project project;
    private final ContextService contextService;
    private Consumer<String> onSendMessage; // 发送消息回调
    private Runnable onContextAdded; // 上下文添加回调

    public ChatInputPanel(Color backgroundColor, Project project, ContextService contextService) {
        this.project = project;
        this.contextService = contextService;

        setLayout(new BorderLayout());
        setBackground(backgroundColor);

        // 创建发送按钮
        sendButton = createSendButton();

        // 创建输入框
        inputField = createInputField(backgroundColor);
        inputScrollPane = createInputScrollPane(backgroundColor);

        // 计算默认高度
        FontMetrics fm = inputField.getFontMetrics(inputField.getFont());
        int lineHeight = fm.getHeight();
        defaultHeight = lineHeight * 2 + inputField.getInsets().top + inputField.getInsets().bottom + JBUI.scale(20);
        inputScrollPane.setPreferredSize(new Dimension(300, defaultHeight));

        // 创建分层面板（按钮浮动在输入框上）
        JLayeredPane container = createLayeredContainer();

        add(container, BorderLayout.CENTER);
        setBorder(JBUI.Borders.empty(4));

        // 设置快捷键
        setupKeyBindings();

        // 设置粘贴监听器（两种方式：KeyBinding + TransferHandler）
        setupPasteListener();
        setupTransferHandler();

        // 监听文本变化，动态调整高度
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateHeight());
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateHeight());
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateHeight());
            }
        });
    }

    /**
     * 创建发送按钮
     */
    private JButton createSendButton() {
        JButton button = new JButton("→");
        button.setBackground(new JBColor(new Color(66, 133, 244), new Color(45, 100, 200)));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Dimension buttonSize = new Dimension(JBUI.scale(32), JBUI.scale(32));
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);

        // 悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new JBColor(new Color(51, 103, 214), new Color(35, 80, 180)));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new JBColor(new Color(66, 133, 244), new Color(45, 100, 200)));
            }
        });

        // 点击事件
        button.addActionListener(e -> sendMessage());

        return button;
    }

    /**
     * 创建输入框
     */
    private JTextArea createInputField(Color backgroundColor) {
        JTextArea field = new JTextArea(2, 12);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        field.setBackground(lightenColor(backgroundColor, 0.05f));
        field.setForeground(JBUI.CurrentTheme.Label.foreground());
        field.setFont(JBUI.Fonts.label().deriveFont(Font.PLAIN, 14f));
        field.setBorder(JBUI.Borders.empty(5, 8, 5, 45));
        return field;
    }

    /**
     * 创建输入框滚动面板
     */
    private JBScrollPane createInputScrollPane(Color backgroundColor) {
        Color inputBg = lightenColor(backgroundColor, 0.05f);
        JBScrollPane scrollPane = new JBScrollPane(inputField);
        scrollPane.setBackground(inputBg);
        scrollPane.getViewport().setBackground(inputBg);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.customLine(lightenColor(backgroundColor, 0.15f), 1));
        return scrollPane;
    }

    /**
     * 创建分层容器（按钮浮动在输入框上）
     */
    private JLayeredPane createLayeredContainer() {
        JLayeredPane container = new JLayeredPane() {
            @Override
            public Dimension getPreferredSize() {
                return inputScrollPane.getPreferredSize();
            }
        };

        container.add(inputScrollPane, JLayeredPane.DEFAULT_LAYER);
        container.add(sendButton, JLayeredPane.PALETTE_LAYER);

        // 监听大小变化，动态调整组件位置
        container.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int width = container.getWidth();
                int height = container.getHeight();

                inputScrollPane.setBounds(0, 0, width, height);

                int buttonWidth = sendButton.getPreferredSize().width;
                int buttonHeight = sendButton.getPreferredSize().height;
                sendButton.setBounds(
                        width - buttonWidth - JBUI.scale(4),
                        height - buttonHeight - JBUI.scale(4),
                        buttonWidth,
                        buttonHeight);
            }
        });

        return container;
    }

    /**
     * 设置快捷键
     */
    private void setupKeyBindings() {
        InputMap inputMap = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = inputField.getActionMap();

        // Enter 键发送
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        actionMap.put("send", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendMessage();
            }
        });

        // Shift+Enter 换行
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");
    }

    /**
     * 更新输入框高度
     */
    private void updateHeight() {
        try {
            FontMetrics fm = inputField.getFontMetrics(inputField.getFont());
            int lineHeight = fm.getHeight();

            // 计算实际行数
            String text = inputField.getText();
            int width = inputField.getWidth() - inputField.getInsets().left - inputField.getInsets().right;
            if (width <= 0)
                width = JBUI.scale(200);

            int totalLines = 0;
            for (String line : text.split("\n", -1)) {
                if (line.isEmpty()) {
                    totalLines++;
                } else {
                    int lineWidth = fm.stringWidth(line);
                    totalLines += Math.max(1, (int) Math.ceil((double) lineWidth / width));
                }
            }
            totalLines = Math.max(1, totalLines);

            // 计算高度
            int contentHeight = totalLines * lineHeight + inputField.getInsets().top + inputField.getInsets().bottom
                    + JBUI.scale(10);
            int targetHeight = Math.max(defaultHeight, Math.min(contentHeight, JBUI.scale(150)));

            if (Math.abs(inputScrollPane.getPreferredSize().height - targetHeight) > 2) {
                inputScrollPane.setPreferredSize(new Dimension(inputScrollPane.getPreferredSize().width, targetHeight));
                inputScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, targetHeight));
                inputScrollPane.revalidate();

                Container parent = inputScrollPane.getParent();
                if (parent != null) {
                    parent.revalidate();
                    if (parent.getParent() != null) {
                        parent.getParent().revalidate();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && onSendMessage != null) {
            onSendMessage.accept(message);
            inputField.setText("");
        }
    }

    /**
     * 设置发送消息回调
     */
    public void setOnSendMessage(Consumer<String> callback) {
        this.onSendMessage = callback;
    }

    /**
     * 设置输入框启用状态
     */
    public void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        sendButton.setCursor(new Cursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /**
     * 设置粘贴监听器（方式1：KeyBinding）
     */
    private void setupPasteListener() {
        // 监听粘贴事件（Ctrl+V）
        // 使用 WHEN_FOCUSED 确保在组件获得焦点时生效
        InputMap inputMap = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = inputField.getActionMap();

        // 绑定 Ctrl+V
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste-with-detection");

        actionMap.put("paste-with-detection", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handlePaste();
            }
        });
    }

    /**
     * 设置 TransferHandler（方式2：更可靠的粘贴拦截）
     */
    private void setupTransferHandler() {
        inputField.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                // 支持字符串导入
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    // 获取粘贴的文本
                    String pastedText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                    // 尝试解析为代码块
                    CodeBlockParser.CodeBlock codeBlock = CodeBlockParser.parse(pastedText, project);

                    if (codeBlock != null && codeBlock.isValid()) {
                        // 识别为代码块，自动添加到上下文
                        handleCodeBlockPaste(codeBlock);
                        return true; // 阻止默认粘贴行为
                    } else {
                        // 不是代码块，执行默认粘贴
                        int caretPosition = inputField.getCaretPosition();
                        inputField.insert(pastedText, caretPosition);
                        return true;
                    }
                } catch (Exception ex) {
                    return false; // 让系统处理默认粘贴
                }
            }
        });
    }

    /**
     * 处理粘贴事件
     */
    private void handlePaste() {
        try {
            // 获取剪贴板内容
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String pastedText = (String) transferable.getTransferData(DataFlavor.stringFlavor);

                // 尝试解析为代码块
                CodeBlockParser.CodeBlock codeBlock = CodeBlockParser.parse(pastedText, project);

                if (codeBlock != null && codeBlock.isValid()) {
                    // 识别为代码块，自动添加到上下文
                    handleCodeBlockPaste(codeBlock);
                } else {
                    // 不是代码块，执行普通粘贴
                    inputField.paste();
                }
            }
        } catch (Exception ex) {
            // 出错时执行普通粘贴
            inputField.paste();
        }
    }

    /**
     * 处理代码块粘贴
     */
    private void handleCodeBlockPaste(CodeBlockParser.CodeBlock codeBlock) {
        if (contextService != null) {
            // 将 CodeBlock 转换为 ContextItem
            ContextService.ContextItem contextItem = createContextItem(codeBlock);

            // 添加到上下文服务
            contextService.addContext(contextItem);

            // 通知上下文已添加（触发UI更新）
            if (onContextAdded != null) {
                SwingUtilities.invokeLater(onContextAdded);
            }

            // 显示提示信息
            String displayName = codeBlock.fileName != null ? codeBlock.fileName : "代码片段";
            showContextAddedHint(displayName);
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
     * 显示上下文添加提示（2秒后自动消失）
     */
    private void showContextAddedHint(String fileName) {
        String currentText = inputField.getText().trim();
        String hintText = "✅ 已添加代码片段到上下文: " + fileName;

        // 添加提示文本
        if (!currentText.isEmpty()) {
            inputField.setText(currentText + "\n" + hintText);
        } else {
            inputField.setText(hintText);
        }

        // 设置定时器清除提示
        Timer clearTimer = new Timer(2000, e -> clearHintText());
        clearTimer.setRepeats(false);
        clearTimer.start();
    }

    /**
     * 清除提示文本
     */
    private void clearHintText() {
        String text = inputField.getText();
        if (text.contains("✅ 已添加代码片段到上下文")) {
            // 移除包含提示的行
            String[] lines = text.split("\n");
            StringBuilder newText = new StringBuilder();

            for (String line : lines) {
                if (!line.contains("✅ 已添加代码片段到上下文")) {
                    if (newText.length() > 0) {
                        newText.append("\n");
                    }
                    newText.append(line);
                }
            }

            inputField.setText(newText.toString().trim());
        }
    }

    /**
     * 设置上下文添加回调
     */
    public void setOnContextAdded(Runnable callback) {
        this.onContextAdded = callback;
    }

    /**
     * 请求输入框焦点
     */
    public void requestInputFocus() {
        inputField.requestFocus();
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
