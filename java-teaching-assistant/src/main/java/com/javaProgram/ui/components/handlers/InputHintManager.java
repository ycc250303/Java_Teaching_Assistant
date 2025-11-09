package com.javaProgram.ui.components.handlers;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * 输入提示管理器
 * 管理输入框中的提示信息显示和清除
 */
public class InputHintManager {
    private static final String ERROR_PREFIX = "❌ 添加失败: ";
    private final JTextComponent inputField;
    private Timer clearTimer;

    public InputHintManager(JTextComponent inputField) {
        this.inputField = inputField;
    }

    /**
     * 显示添加失败提示（2秒后自动消失）
     *
     * @param message 错误消息
     */
    public void showErrorHint(String message) {
        // 取消之前的定时器
        if (clearTimer != null && clearTimer.isRunning()) {
            clearTimer.stop();
        }

        String currentText = inputField.getText().trim();
        String hintText = ERROR_PREFIX + message;

        // 添加提示文本
        if (!currentText.isEmpty()) {
            inputField.setText(currentText + "\n" + hintText);
        } else {
            inputField.setText(hintText);
        }

        // 设置定时器清除提示
        clearTimer = new Timer(2000, e -> clearHintText());
        clearTimer.setRepeats(false);
        clearTimer.start();
    }

    /**
     * 清除提示文本
     */
    private void clearHintText() {
        String text = inputField.getText();
        if (text.contains(ERROR_PREFIX)) {
            // 移除包含提示的行
            String[] lines = text.split("\n");
            StringBuilder newText = new StringBuilder();

            for (String line : lines) {
                if (!line.contains(ERROR_PREFIX)) {
                    if (newText.length() > 0) {
                        newText.append("\n");
                    }
                    newText.append(line);
                }
            }

            inputField.setText(newText.toString().trim());
        }
    }
}
