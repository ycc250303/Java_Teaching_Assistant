package com.example.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ChatToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建聊天窗口UI
        ChatToolWindowContent chatToolWindowContent = new ChatToolWindowContent(project);

        // 获取内容工厂
        ContentFactory contentFactory = ContentFactory.getInstance();

        // 创建内容
        Content content = contentFactory.createContent(chatToolWindowContent.getContent(), "", false);

        // 添加内容到工具窗口
        toolWindow.getContentManager().addContent(content);
    }
}