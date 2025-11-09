package com.javaProgram.ui.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.JBColor;
import com.javaProgram.services.ContextService;
import com.javaProgram.ui.components.handlers.CodeBlockPasteHandler;
import com.javaProgram.ui.components.handlers.FileDropHandler;
import com.javaProgram.ui.components.handlers.InputHintManager;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

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

    // 处理器
    private final FileDropHandler fileDropHandler;
    private final CodeBlockPasteHandler codeBlockPasteHandler;
    private final InputHintManager hintManager;

    // 回调
    private Consumer<String> onSendMessage;
    private Runnable onContextAdded;

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

        // 初始化处理器
        hintManager = new InputHintManager(inputField);
        fileDropHandler = new FileDropHandler(project, contextService,
                hintManager::showErrorHint, this::notifyContextAdded);
        codeBlockPasteHandler = new CodeBlockPasteHandler(project, contextService,
                hintManager::showErrorHint, this::notifyContextAdded);

        // 设置快捷键
        setupKeyBindings();

        // 设置粘贴监听器
        setupPasteListener();

        // 设置拖拽和粘贴处理器
        setupTransferHandler();

        // 监听文本变化，动态调整高度
        setupDocumentListener();
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
     * 设置文档监听器
     */
    private void setupDocumentListener() {
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
     * 设置粘贴监听器
     */
    private void setupPasteListener() {
        InputMap inputMap = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = inputField.getActionMap();

        // 绑定 Ctrl+V
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste-with-detection");

        actionMap.put("paste-with-detection", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!codeBlockPasteHandler.handlePaste()) {
                    // 如果不是代码块，执行普通粘贴
                    inputField.paste();
                }
            }
        });
    }

    /**
     * 设置 TransferHandler（拖拽和粘贴支持）
     */
    private void setupTransferHandler() {
        inputField.setTransferHandler(new javax.swing.TransferHandler() {
            @Override
            public boolean canImport(javax.swing.TransferHandler.TransferSupport support) {
                // 支持字符串导入
                if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return true;
                }

                // 支持文件列表拖拽（从文件系统）
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return true;
                }

                // 支持IntelliJ的VirtualFile拖拽（从Project视图）
                try {
                    java.awt.datatransfer.Transferable transferable = support.getTransferable();
                    if (transferable != null) {
                        DataFlavor[] flavors = transferable.getTransferDataFlavors();
                        for (DataFlavor flavor : flavors) {
                            if (flavor.getHumanPresentableName().contains("VirtualFile") ||
                                    flavor.getMimeType().contains("virtualfile")) {
                                return true;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                return false;
            }

            @Override
            public boolean importData(javax.swing.TransferHandler.TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    java.awt.datatransfer.Transferable transferable = support.getTransferable();

                    // 1. 优先处理文件拖拽
                    if (fileDropHandler.handleFileDrop(transferable)) {
                        return true;
                    }

                    // 2. 处理文本粘贴
                    if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String pastedText = (String) transferable.getTransferData(DataFlavor.stringFlavor);

                        if (codeBlockPasteHandler.handlePastedText(pastedText)) {
                            // 识别为代码块，已添加到上下文
                            return true;
                        } else {
                            // 不是代码块，执行默认粘贴
                            int caretPosition = inputField.getCaretPosition();
                            inputField.insert(pastedText, caretPosition);
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    return false;
                }

                return false;
            }
        });
    }

    /**
     * 通知上下文已添加
     */
    private void notifyContextAdded() {
        if (onContextAdded != null) {
            SwingUtilities.invokeLater(onContextAdded);
        }
    }

    /**
     * 设置发送消息回调
     */
    public void setOnSendMessage(Consumer<String> callback) {
        this.onSendMessage = callback;
    }

    /**
     * 设置上下文添加回调
     */
    public void setOnContextAdded(Runnable callback) {
        this.onContextAdded = callback;
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
     * 请求输入框焦点
     */
    public void requestInputFocus() {
        inputField.requestFocus();
    }

    /**
     * 清空输入框内容
     */
    public void clearInput() {
        inputField.setText("");
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
