package com.javaProgram.ui.components.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.javaProgram.services.ContextService;
import com.javaProgram.utils.FileContentReader;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

/**
 * 文件拖拽处理器
 * 处理从Project视图和文件系统拖拽的文件
 */
public class FileDropHandler {
    private final Project project;
    private final ContextService contextService;
    private final Consumer<String> onErrorHint; // 显示错误提示的回调
    private final Runnable onContextAdded; // 上下文添加回调

    // 支持的文件扩展名
    private static final String[] SUPPORTED_EXTENSIONS = {
            "java", "kt", "py", "js", "ts", "xml", "json", "yaml", "yml",
            "md", "txt", "properties", "gradle", "sql", "sh", "bat"
    };

    public FileDropHandler(Project project, ContextService contextService,
            Consumer<String> onErrorHint, Runnable onContextAdded) {
        this.project = project;
        this.contextService = contextService;
        this.onErrorHint = onErrorHint;
        this.onContextAdded = onContextAdded;
    }

    /**
     * 处理文件拖拽
     *
     * @param transferable 传输对象
     * @return 是否成功处理
     */
    public boolean handleFileDrop(Transferable transferable) {
        if (contextService == null || project == null) {
            return false;
        }

        try {
            // 1. 尝试从IntelliJ的VirtualFile获取（从Project视图拖拽）
            VirtualFile[] virtualFiles = getVirtualFilesFromTransferable(transferable);
            if (virtualFiles != null && virtualFiles.length > 0) {
                return handleVirtualFiles(virtualFiles);
            }

            // 2. 尝试从文件系统获取（从文件管理器拖拽）
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files != null && !files.isEmpty()) {
                    return handleSystemFiles(files);
                }
            }
        } catch (Exception ex) {
            // 忽略异常，返回false让系统处理
            return false;
        }

        return false;
    }

    /**
     * 从Transferable获取VirtualFile数组
     */
    private VirtualFile[] getVirtualFilesFromTransferable(Transferable transferable) {
        try {
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            for (DataFlavor flavor : flavors) {
                try {
                    Object data = transferable.getTransferData(flavor);
                    if (data instanceof VirtualFile[]) {
                        return (VirtualFile[]) data;
                    } else if (data instanceof VirtualFile) {
                        return new VirtualFile[] { (VirtualFile) data };
                    }
                    // 尝试通过反射获取（如果IntelliJ使用了特定的包装类）
                    if (data != null) {
                        Class<?> dataClass = data.getClass();
                        if (dataClass.isArray() && VirtualFile.class.isAssignableFrom(dataClass.getComponentType())) {
                            return (VirtualFile[]) data;
                        }
                    }
                } catch (Exception ignored) {
                    // 某些DataFlavor可能无法直接获取，继续尝试下一个
                }
            }

            // 如果直接获取失败，尝试通过文件路径查找
            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String pathData = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    if (pathData != null && (pathData.contains("\\") || pathData.contains("/"))) {
                        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
                        VirtualFile vf = fileSystem.findFileByPath(pathData.trim());
                        if (vf != null && !vf.isDirectory()) {
                            return new VirtualFile[] { vf };
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * 处理VirtualFile数组（从Project视图拖拽）
     */
    private boolean handleVirtualFiles(VirtualFile[] virtualFiles) {
        int successCount = 0;

        for (VirtualFile virtualFile : virtualFiles) {
            if (virtualFile.isDirectory()) {
                continue;
            }

            if (!FileContentReader.isSupportedFile(virtualFile)) {
                continue;
            }

            String content = FileContentReader.readFileContent(virtualFile, project);
            if (content == null || content.trim().isEmpty()) {
                continue;
            }

            String[] lines = content.split("\n");
            ContextService.ContextItem contextItem = new ContextService.ContextItem(
                    virtualFile.getName(),
                    virtualFile.getPath(),
                    1,
                    lines.length,
                    content);

            contextService.addContext(contextItem);
            successCount++;
        }

        if (successCount > 0) {
            notifyContextAdded();
            return true;
        } else {
            // 所有文件都处理失败，显示错误提示
            showErrorHint("无法添加文件到上下文（文件类型不支持或读取失败）");
            return false;
        }
    }

    /**
     * 处理系统文件列表（从文件管理器拖拽）
     */
    private boolean handleSystemFiles(List<File> files) {
        int successCount = 0;

        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }

            if (!isSupportedFile(file)) {
                continue;
            }

            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                if (content.trim().isEmpty()) {
                    continue;
                }

                String[] lines = content.split("\n");
                ContextService.ContextItem contextItem = new ContextService.ContextItem(
                        file.getName(),
                        file.getAbsolutePath(),
                        1,
                        lines.length,
                        content);

                contextService.addContext(contextItem);
                successCount++;
            } catch (Exception ex) {
                // 忽略读取失败的文件
                continue;
            }
        }

        if (successCount > 0) {
            notifyContextAdded();
            return true;
        } else {
            // 所有文件都处理失败，显示错误提示
            showErrorHint("无法添加文件到上下文（文件类型不支持或读取失败）");
            return false;
        }
    }

    /**
     * 检查文件是否支持
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName().toLowerCase();
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            extension = fileName.substring(lastDot + 1);
        }

        for (String ext : SUPPORTED_EXTENSIONS) {
            if (extension.equals(ext)) {
                return true;
            }
        }

        return false;
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
     * 显示错误提示信息
     */
    private void showErrorHint(String message) {
        if (onErrorHint != null) {
            onErrorHint.accept(message);
        }
    }
}
