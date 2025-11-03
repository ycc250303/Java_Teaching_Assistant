package com.javaProgram.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

public class ChatToolWindowFactory implements ToolWindowFactory {

    // 定义用户数据的Key（使用 Key<T> 而不是 String）
    private static final Key<ChatToolWindowContent> CHAT_CONTENT_KEY = Key.create("ChatToolWindowContent");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 设置工具窗口标题
        toolWindow.setTitle("智能助手");

        // 创建聊天窗口UI
        ChatToolWindowContent chatToolWindowContent = new ChatToolWindowContent(project);

        // 获取内容工厂
        ContentFactory contentFactory = ContentFactory.getInstance();

        // 创建内容
        Content content = contentFactory.createContent(chatToolWindowContent.getContent(), "智能助手", false);

        // 将 ChatToolWindowContent 实例保存到 Content 的用户数据中
        content.putUserData(CHAT_CONTENT_KEY, chatToolWindowContent);

        // 添加内容到工具窗口
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 从项目获取 ChatToolWindowContent 实例的辅助方法
     */
    public static ChatToolWindowContent getChatContent(@NotNull Project project) {
        if (project == null) {
            return null;
        }

        ToolWindow toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                .getToolWindow("智能会话");

        if (toolWindow == null) {
            return null;
        }

        Content[] contents = toolWindow.getContentManager().getContents();
        if (contents.length == 0) {
            return null;
        }

        return contents[0].getUserData(CHAT_CONTENT_KEY);
    }
}