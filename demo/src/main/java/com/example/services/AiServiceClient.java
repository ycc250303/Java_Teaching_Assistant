package com.example.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AiServiceClient {
    private static final String BASE_URL = "http://localhost:8081/api/ai/chat";
    private int memoryId;

    public AiServiceClient(int memoryId) {
        this.memoryId = memoryId;
    }

    /**
     * 发送聊天消息并流式接收响应
     * 
     * @param message    用户消息
     * @param onChunk    接收到数据块时的回调
     * @param onComplete 完成时的回调
     * @param onError    出错时的回调
     */
    public void sendMessage(String message, Consumer<String> onChunk,
            Runnable onComplete, Consumer<String> onError) {

        // ========== 第1步：在后台线程执行网络请求 ==========
        // 为什么要用后台线程？
        // - 网络请求是阻塞操作，会卡住 UI
        // - IntelliJ Platform 要求不能在 EDT（事件分发线程）中执行耗时操作
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // ========== 第2步：构建请求 URL ==========
                // URL 编码：将特殊字符转换为 %XX 格式
                // 例如：空格 → %20, 中文"你好" → %E4%BD%A0%E5%A5%BD
                String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

                // 拼接完整 URL：
                // http://localhost:8081/api/ai/chat?memoryId=123&message=你好
                String urlStr = BASE_URL + "?memoryId=" + memoryId + "&message=" + encodedMessage;

                // ========== 第3步：建立 HTTP 连接 ==========
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法为 GET
                connection.setRequestMethod("GET");

                // 设置 Accept 头，告诉服务器我们接受 SSE 格式
                connection.setRequestProperty("Accept", "text/event-stream");

                // 连接超时：5秒内必须建立连接
                connection.setConnectTimeout(5000);

                // 读取超时：30秒内必须有数据传输
                connection.setReadTimeout(30000);

                // ========== 第4步：检查响应状态码 ==========
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // HTTP 200 - 成功

                    // ========== 第5步：读取流式响应 ==========
                    // BufferedReader 用于逐行读取数据
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                        String line;
                        // 循环读取每一行，直到流结束
                        while ((line = reader.readLine()) != null) {
                            // SSE 格式：每行以 "data:" 开头
                            if (line.startsWith("data:")) {
                                // 提取实际数据（去掉 "data:" 前缀）
                                String data = line.substring(5).trim();

                                if (!data.isEmpty()) {
                                    // ========== 第6步：在 UI 线程更新界面 ==========
                                    // 为什么要切换到 UI 线程？
                                    // - Swing 组件不是线程安全的
                                    // - 必须在 EDT 线程中操作 UI 组件
                                    ApplicationManager.getApplication().invokeLater(() -> onChunk.accept(data) // 调用回调函数
                                    );
                                }
                            }
                        }

                        // ========== 第7步：完成回调 ==========
                        // 所有数据接收完毕，在 UI 线程调用完成回调
                        ApplicationManager.getApplication().invokeLater(onComplete);
                    }
                } else {
                    // HTTP 错误（如 404, 500 等）
                    String error = "HTTP Error: " + connection.getResponseCode();
                    ApplicationManager.getApplication().invokeLater(() -> onError.accept(error));
                }

            } catch (IOException e) {
                // ========== 第8步：异常处理 ==========
                // 捕获所有 IO 异常（网络错误、超时等）
                ApplicationManager.getApplication().invokeLater(() -> onError.accept("连接失败: " + e.getMessage()));
            }
        });
    }

    public void setMemoryId(int memoryId) {
        this.memoryId = memoryId;
    }

    public int getMemoryId() {
        return memoryId;
    }
}
