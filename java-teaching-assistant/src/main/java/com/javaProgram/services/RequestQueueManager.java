package com.javaProgram.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 请求队列管理器
 * 职责：管理AI请求队列，支持异步处理多个用户请求
 */
public class RequestQueueManager {
    private static final int MAX_QUEUE_SIZE = 3;

    // 队列相关
    private final Queue<QueuedRequest> requestQueue = new LinkedList<>();
    private QueuedRequest currentRequest = null;
    private boolean isProcessing = false;

    // 线程同步锁
    private final Object lock = new Object();

    // 监听器列表（用于UI更新）
    private final List<QueueChangeListener> listeners = new ArrayList<>();

    // 请求处理回调
    private Consumer<QueuedRequest> processCallback;

    /**
     * 请求对象
     */
    public static class QueuedRequest {
        private final String id;
        private final String message;
        private final List<ContextService.ContextItem> contextList;
        private final long timestamp;
        private RequestStatus status;

        public QueuedRequest(String message, List<ContextService.ContextItem> contextList) {
            this.id = UUID.randomUUID().toString();
            this.message = message;
            // 创建上下文的深拷贝，避免被清除
            this.contextList = contextList != null ? new ArrayList<>(contextList) : null;
            this.timestamp = System.currentTimeMillis();
            this.status = RequestStatus.WAITING;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }

        public List<ContextService.ContextItem> getContextList() {
            return contextList;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public RequestStatus getStatus() {
            return status;
        }

        public void setStatus(RequestStatus status) {
            this.status = status;
        }

        public enum RequestStatus {
            WAITING, // 等待中
            PROCESSING, // 处理中
            COMPLETED, // 已完成
            FAILED // 失败
        }
    }

    /**
     * 队列变化监听器
     */
    public interface QueueChangeListener {
        void onQueueChanged(List<QueuedRequest> queue, QueuedRequest current);
    }

    /**
     * 添加请求到队列
     * 
     * @param message 用户消息
     * @param context 上下文列表（会创建副本）
     * @return true表示添加成功，false表示队列已满
     */
    public boolean addRequest(String message, List<ContextService.ContextItem> context) {
        synchronized (lock) {
            if (requestQueue.size() >= MAX_QUEUE_SIZE) {
                return false;
            }

            QueuedRequest request = new QueuedRequest(message, context);
            requestQueue.offer(request);
            notifyListeners();
            return true;
        }
    }

    /**
     * 开始处理队列（处理第一个请求）
     */
    public void startProcessing() {
        synchronized (lock) {
            if (isProcessing || requestQueue.isEmpty()) {
                return;
            }

            processNextRequest();
        }
    }

    /**
     * 完成当前请求，自动处理下一个
     */
    public void completeCurrentRequest() {
        synchronized (lock) {
            if (currentRequest != null) {
                currentRequest.setStatus(QueuedRequest.RequestStatus.COMPLETED);
                System.out
                        .println("请求完成: " + currentRequest.getId() + " - " + truncate(currentRequest.getMessage(), 30));
                currentRequest = null;
            }

            processNextRequest();
        }
    }

    /**
     * 标记当前请求失败，自动处理下一个
     */
    public void failCurrentRequest() {
        synchronized (lock) {
            if (currentRequest != null) {
                currentRequest.setStatus(QueuedRequest.RequestStatus.FAILED);
                System.err
                        .println("请求失败: " + currentRequest.getId() + " - " + truncate(currentRequest.getMessage(), 30));
                currentRequest = null;
            }

            processNextRequest();
        }
    }

    /**
     * 处理队列中的下一个请求
     */
    private void processNextRequest() {
        // 注意：此方法必须在 synchronized (lock) 内调用

        if (requestQueue.isEmpty()) {
            isProcessing = false;
            notifyListeners();
            System.out.println("队列已空，所有请求处理完成");
            return;
        }

        // 取出队列头部的请求
        currentRequest = requestQueue.poll();
        currentRequest.setStatus(QueuedRequest.RequestStatus.PROCESSING);
        isProcessing = true;

        System.out.println("开始处理请求: " + currentRequest.getId() + " - " + truncate(currentRequest.getMessage(), 30));

        // 通知监听器（更新UI）
        notifyListeners();

        // 触发处理回调（在同步块外执行，避免死锁）
        final QueuedRequest requestToProcess = currentRequest;
        if (processCallback != null) {
            // 使用新线程触发回调，避免阻塞
            new Thread(() -> processCallback.accept(requestToProcess)).start();
        }
    }

    /**
     * 检查是否正在处理请求
     */
    public boolean isProcessing() {
        synchronized (lock) {
            return isProcessing;
        }
    }

    /**
     * 获取当前正在处理的请求
     */
    public QueuedRequest getCurrentRequest() {
        synchronized (lock) {
            return currentRequest;
        }
    }

    /**
     * 获取等待队列的副本
     */
    public List<QueuedRequest> getQueueSnapshot() {
        synchronized (lock) {
            return new ArrayList<>(requestQueue);
        }
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        synchronized (lock) {
            return requestQueue.size();
        }
    }

    /**
     * 清空队列（可选，用于重置）
     */
    public void clearQueue() {
        synchronized (lock) {
            requestQueue.clear();
            notifyListeners();
        }
    }

    /**
     * 设置请求处理回调
     */
    public void setOnProcessRequest(Consumer<QueuedRequest> callback) {
        this.processCallback = callback;
    }

    /**
     * 添加队列变化监听器
     */
    public void addListener(QueueChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * 移除监听器
     */
    public void removeListener(QueueChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners() {
        List<QueuedRequest> queueSnapshot = new ArrayList<>(requestQueue);
        QueuedRequest current = currentRequest;

        synchronized (listeners) {
            for (QueueChangeListener listener : listeners) {
                try {
                    listener.onQueueChanged(queueSnapshot, current);
                } catch (Exception e) {
                    System.err.println("通知监听器失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
