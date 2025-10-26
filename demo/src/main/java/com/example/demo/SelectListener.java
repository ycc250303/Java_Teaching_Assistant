package com.example.demo;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.wm.IdeFrame;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.KeyEvent;

public class SelectListener implements ApplicationActivationListener {
    private final KeyEventDispatcher dispatcher = new MyKeyEventDispatcher();

    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }


    @Override
    public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
    }

    private static class MyKeyEventDispatcher implements KeyEventDispatcher {

        private boolean shiftPressed = false;

        private boolean ctrlPressed = false;

        private boolean winPressed = false;

        private final EditorSettingsExternalizable editorSettingsExternalizable = EditorSettingsExternalizable.getInstance();

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            int keyCode = e.getKeyCode();
            int id = e.getID();

            if (id == KeyEvent.KEY_PRESSED) {
                switch (keyCode) {
                    case KeyEvent.VK_SHIFT:
                        shiftPressed = true;
                        break;
                    case KeyEvent.VK_CONTROL:
                        ctrlPressed = true;
                        break;
                    case KeyEvent.VK_WINDOWS:
                        winPressed = true;
                        break;
                }
                if (shiftPressed && ctrlPressed && winPressed) {
                    System.out.println("Shift + Ctrl + Win 按下");
                    editorSettingsExternalizable.setCamelWords(true);
                }
            } else if (id == KeyEvent.KEY_RELEASED) {
                switch (keyCode) {
                    case KeyEvent.VK_SHIFT:
                        shiftPressed = false;
                        break;
                    case KeyEvent.VK_CONTROL:
                        ctrlPressed = false;
                        break;
                    case KeyEvent.VK_WINDOWS:
                        if (shiftPressed && ctrlPressed) {
                            System.out.println("Win 释放");
                            editorSettingsExternalizable.setCamelWords(false);
                        }
                        winPressed = false;
                        break;
                        //D:\GitHub\ycc\Java_Enterprise_Application_Development\ClassTest\GarfieldTaskAppDemo
                }
            }
            return false;
        }
    }
}
