package com.javaProgram.services;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 上下文服务，用于管理用户添加的代码上下文
 */
@Service(Service.Level.PROJECT)
@State(name = "ContextService", storages = @Storage("contextService.xml"))
public final class ContextService implements PersistentStateComponent<ContextService.State> {

    /**
     * 上下文项数据结构
     */
    public static class ContextItem {
        private String fileName;
        private String filePath;
        private int startLine;
        private int endLine;
        private String content;

        public ContextItem() {
        }

        public ContextItem(String fileName, String filePath, int startLine, int endLine, String content) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.startLine = startLine;
            this.endLine = endLine;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        /**
         * 获取显示的行数范围文本
         */
        public String getLineRangeText() {
            if (startLine == endLine) {
                return "Line " + startLine;
            } else {
                return "Lines " + startLine + "-" + endLine;
            }
        }
    }

    private final List<ContextItem> contextList;
    private String combinedContext;
    private List<ContextListener> listeners;

    public ContextService() {
        this.contextList = new CopyOnWriteArrayList<>();
        this.combinedContext = "";
        this.listeners = new CopyOnWriteArrayList<>();
    }

    // 状态类，用于持久化
    public static class State {
        public List<ContextItem> contextList = new ArrayList<>();
    }

    /**
     * 添加上下文（旧方法，保持兼容性）
     * @deprecated 使用 addContext(ContextItem) 代替
     */
    @Deprecated
    public void addContext(@NotNull String context) {
        // 为了保持兼容性，创建一个简单的ContextItem
        ContextItem item = new ContextItem("Unknown", "", 0, 0, context);
        contextList.add(item);
        updateCombinedContext();
        notifyListeners();
    }

    /**
     * 添加结构化上下文
     */
    public void addContext(@NotNull ContextItem contextItem) {
        contextList.add(contextItem);
        updateCombinedContext();
        notifyListeners();
    }

    /**
     * 清空所有上下文
     */
    public void clearContext() {
        contextList.clear();
        combinedContext = "";
        notifyListeners();
    }

    /**
     * 获取当前所有上下文
     */
    public String getCurrentContext() {
        return combinedContext;
    }

    /**
     * 获取上下文列表的副本
     */
    public List<ContextItem> getContextList() {
        return new ArrayList<>(contextList);
    }

    /**
     * 移除指定索引的上下文
     */
    public void removeContext(int index) {
        if (index >= 0 && index < contextList.size()) {
            contextList.remove(index);
            updateCombinedContext();
            notifyListeners();
        }
    }

    /**
     * 添加上下文变更监听器
     */
    public void addContextListener(@NotNull ContextListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除上下文变更监听器
     */
    public void removeContextListener(@NotNull ContextListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners() {
        for (ContextListener listener : listeners) {
            try {
                listener.onContextChanged(combinedContext);
            } catch (Exception e) {
                // 忽略监听器异常，避免影响其他监听器
            }
        }
    }

    /**
     * 更新合并后的上下文
     */
    private void updateCombinedContext() {
        if (contextList.isEmpty()) {
            combinedContext = "";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 用户提供的代码上下文 ===\n\n");

            for (int i = 0; i < contextList.size(); i++) {
                ContextItem item = contextList.get(i);
                sb.append("上下文 ").append(i + 1).append(":\n");
                sb.append("文件: ").append(item.getFileName());
                if (item.getStartLine() > 0) {
                    sb.append(" (").append(item.getLineRangeText()).append(")");
                }
                sb.append("\n\n选中的代码:\n");
                sb.append(item.getContent());
                if (i < contextList.size() - 1) {
                    sb.append("\n\n---\n\n");
                }
            }

            sb.append("\n=== 上下文结束 ===\n\n");
            combinedContext = sb.toString();
        }
    }

    @Override
    public @Nullable State getState() {
        State state = new State();
        state.contextList = new ArrayList<>(this.contextList);
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        // 每次启动时清空上下文，不加载持久化的数据
        this.contextList.clear();
        this.combinedContext = "";
        // 注释掉下面这行，避免加载之前的上下文
        // this.contextList.addAll(state.contextList);
        // updateCombinedContext();
    }

    /**
     * 上下文变更监听器接口
     */
    public interface ContextListener {
        void onContextChanged(@NotNull String newContext);
    }
}