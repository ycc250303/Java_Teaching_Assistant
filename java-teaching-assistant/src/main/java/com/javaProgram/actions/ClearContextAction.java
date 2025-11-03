package com.javaProgram.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.javaProgram.services.ContextService;
import org.jetbrains.annotations.NotNull;

public class ClearContextAction extends AnAction {

    public ClearContextAction() {
        super("清空AI上下文");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ContextService contextService = ServiceManager.getService(project, ContextService.class);

        if (contextService != null) {
            int contextCount = contextService.getContextList().size();

            if (contextCount == 0) {
                Messages.showInfoMessage(project, "当前没有任何上下文需要清空", "提示");
                return;
            }

            int result = Messages.showYesNoDialog(
                project,
                "确定要清空所有已添加的上下文吗？\n\n当前有 " + contextCount + " 个上下文项。",
                "清空上下文",
                Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                contextService.clearContext();
                Messages.showInfoMessage(project, "已成功清空所有上下文", "清空成功");
            }
        } else {
            Messages.showErrorDialog(project, "无法获取上下文服务", "错误");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && ServiceManager.getService(project, ContextService.class) != null;
        e.getPresentation().setEnabled(enabled);
    }
}