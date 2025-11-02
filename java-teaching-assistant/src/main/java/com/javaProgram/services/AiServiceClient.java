package com.javaProgram.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AiServiceClient {
    private static final String BASE_URL = "http://localhost:8081/api/ai/chat";
    private static final String MODIFY_CODE_URL = "http://localhost:8081/api/ai/modify-code";
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

    /**
     * 请求AI修改代码
     *
     * @param originalCode 原始代码
     * @param instruction  修改指令
     * @param fileName     文件名（可选）
     * @param onSuccess    成功回调，接收修改后的代码
     * @param onError      失败回调，接收错误信息
     */
    public void modifyCode(String originalCode, String instruction, String fileName,
                           Consumer<String> onSuccess, Consumer<String> onError) {

        // ========== 第1步：在后台线程执行网络请求 ==========
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // ========== 第2步：构建JSON请求体 ==========
                // 手动构建JSON字符串（转义特殊字符）
                String jsonBody = buildJsonRequest(originalCode, instruction, fileName);

                // ========== 第3步：建立HTTP连接 ==========
                URL url = new URL(MODIFY_CODE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法为POST
                connection.setRequestMethod("POST");

                // 设置请求头
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");

                // 允许输出
                connection.setDoOutput(true);

                // 连接超时：5秒内必须建立连接
                connection.setConnectTimeout(5000);

                // 读取超时：60秒内必须有响应（代码修改可能需要更长时间）
                connection.setReadTimeout(60000);

                // ========== 第4步：发送请求体 ==========
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(jsonBody);
                    writer.flush();
                }

                // ========== 第5步：检查响应状态码 ==========
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // ========== 第6步：读取响应 ==========
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        // ========== 第7步：解析JSON响应 ==========
                        String responseBody = response.toString();
                        String modifiedCode = parseJsonResponse(responseBody);

                        if (modifiedCode != null) {
                            // 成功，在UI线程调用成功回调
                            ApplicationManager.getApplication().invokeLater(() -> onSuccess.accept(modifiedCode));
                        } else {
                            // 解析失败或返回错误
                            String error = extractErrorFromResponse(responseBody);
                            ApplicationManager.getApplication().invokeLater(() -> onError.accept(error));
                        }
                    }
                } else {
                    // HTTP错误
                    String error = "HTTP Error: " + responseCode;
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorBody = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorBody.append(line);
                        }
                        if (errorBody.length() > 0) {
                            error += ": " + errorBody.toString();
                        }
                    } catch (Exception ignored) {
                        // 忽略读取错误流的异常
                    }
                    final String finalError = error;
                    ApplicationManager.getApplication().invokeLater(() -> onError.accept(finalError));
                }

            } catch (IOException e) {
                // ========== 第8步：异常处理 ==========
                ApplicationManager.getApplication().invokeLater(() ->
                        onError.accept("连接失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 构建JSON请求体
     * 手动构建JSON字符串，转义特殊字符
     */
    private String buildJsonRequest(String originalCode, String instruction, String fileName) {
        // 转义JSON特殊字符
        String escapedCode = escapeJson(originalCode);
        String escapedInstruction = escapeJson(instruction);
        String escapedFileName = fileName != null ? escapeJson(fileName) : "";

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"originalCode\":\"").append(escapedCode).append("\",");
        json.append("\"instruction\":\"").append(escapedInstruction).append("\"");
        if (escapedFileName != null && !escapedFileName.isEmpty()) {
            json.append(",\"fileName\":\"").append(escapedFileName).append("\"");
        }
        json.append("}");

        return json.toString();
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 解析JSON响应，提取修改后的代码
     */
    private String parseJsonResponse(String jsonResponse) {
        try {
            // 简单的JSON解析：查找 "modifiedCode" 字段
            // 格式：{"modifiedCode":"...", "status":"success"}
            int startIndex = jsonResponse.indexOf("\"modifiedCode\":\"");
            if (startIndex == -1) {
                return null;
            }

            startIndex += 16; // 跳过 "modifiedCode":"

            // 找到结束引号（需要考虑转义字符）
            StringBuilder result = new StringBuilder();
            boolean escaped = false;
            for (int i = startIndex; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);

                if (escaped) {
                    // 处理转义字符
                    switch (c) {
                        case 'n':
                            result.append('\n');
                            break;
                        case 'r':
                            result.append('\r');
                            break;
                        case 't':
                            result.append('\t');
                            break;
                        case '\\':
                            result.append('\\');
                            break;
                        case '"':
                            result.append('"');
                            break;
                        default:
                            result.append(c);
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    // 找到结束引号
                    break;
                } else {
                    result.append(c);
                }
            }

            return result.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从JSON响应中提取错误信息
     */
    private String extractErrorFromResponse(String jsonResponse) {
        try {
            // 查找 "error" 字段
            int startIndex = jsonResponse.indexOf("\"error\":\"");
            if (startIndex == -1) {
                return "未知错误";
            }

            startIndex += 9; // 跳过 "error":"

            // 找到结束引号
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return "解析错误信息失败";
            }

            String error = jsonResponse.substring(startIndex, endIndex);
            // 反转义
            return error.replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            return "解析错误信息失败: " + e.getMessage();
        }
    }

    public void setMemoryId(int memoryId) {
        this.memoryId = memoryId;
    }

    public int getMemoryId() {
        return memoryId;
    }
}
