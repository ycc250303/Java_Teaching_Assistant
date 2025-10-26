package com.example.demo;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.DumbAwareAction;

public class SelectAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        System.out.println("SelectAction");
        EditorSettingsExternalizable editorSettingsExternalizable = EditorSettingsExternalizable.getInstance();
        editorSettingsExternalizable.setCamelWords(!editorSettingsExternalizable.isCamelWords());
        System.out.println("open status："+editorSettingsExternalizable.isCamelWords());
    }
}