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
    private static final String MODIFY_CODE_WITH_DIFF_URL = "http://localhost:8081/api/ai/modify-code-with-diff";
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
                // ========== 第2步：构建请求 URL（不带消息参数）==========
                // 改用 POST 方法，避免 GET URL 长度限制
                // GET 限制：通常 2048-8192 字符
                // POST 限制：几乎无限制（取决于服务器配置）
                String urlStr = BASE_URL + "?memoryId=" + memoryId;

                // ========== 第3步：建立 HTTP 连接 ==========
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法为 POST（而不是 GET）
                connection.setRequestMethod("POST");

                // 允许向连接写入数据（POST 请求体）
                connection.setDoOutput(true);

                // 设置请求头
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                // 连接超时：5秒内必须建立连接
                connection.setConnectTimeout(5000);

                // 读取超时：60秒
                connection.setReadTimeout(60000);

                // ========== 第4步：写入请求体（消息内容）==========
                // 将消息放在 POST 请求体中，而不是 URL 参数中
                String postData = "message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(),
                        StandardCharsets.UTF_8)) {
                    writer.write(postData);
                    writer.flush();
                }

                // ========== 第5步：检查响应状态码 ==========
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // HTTP 200 - 成功

                    // ========== 第6步：读取流式响应 ==========
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
                                    // ========== 第7步：在 UI 线程更新界面 ==========
                                    // 为什么要切换到 UI 线程？
                                    // - Swing 组件不是线程安全的
                                    // - 必须在 EDT 线程中操作 UI 组件
                                    ApplicationManager.getApplication().invokeLater(() -> onChunk.accept(data) // 调用回调函数
                                    );
                                }
                            }
                        }

                        // ========== 第8步：完成回调 ==========
                        // 所有数据接收完毕，在 UI 线程调用完成回调
                        ApplicationManager.getApplication().invokeLater(onComplete);
                    }
                } else {
                    // HTTP 错误（如 404, 500 等）
                    String error = "HTTP Error: " + connection.getResponseCode();
                    ApplicationManager.getApplication().invokeLater(() -> onError.accept(error));
                }

            } catch (IOException e) {
                // ========== 第9步：异常处理 ==========
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
                connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept-Charset", "UTF-8");

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
                ApplicationManager.getApplication().invokeLater(() -> onError.accept("连接失败: " + e.getMessage()));
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
                        case 'u':
                            // 处理Unicode转义字符，如 \u4e2d\u6587
                            if (i + 4 < jsonResponse.length()) {
                                String hexStr = jsonResponse.substring(i + 1, i + 5);
                                try {
                                    int unicodeValue = Integer.parseInt(hexStr, 16);
                                    result.append((char) unicodeValue);
                                    i += 4; // 跳过4位十六进制数
                                } catch (NumberFormatException e) {
                                    // 如果解析失败，保持原样
                                    result.append(c);
                                }
                            } else {
                                result.append(c);
                            }
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

    /**
     * 请求AI修改代码（带差异比较）
     *
     * @param originalCode 原始代码
     * @param instruction  修改指令
     * @param fileName     文件名（可选）
     * @param onSuccess    成功回调，接收差异结果
     * @param onError      失败回调，接收错误信息
     */
    public void modifyCodeWithDiff(String originalCode, String instruction, String fileName,
            Consumer<CodeDiffResult> onSuccess, Consumer<String> onError) {

        // ========== 第1步：在后台线程执行网络请求 ==========
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // ========== 第2步：构建JSON请求体 ==========
                String jsonBody = buildJsonRequest(originalCode, instruction, fileName);

                // ========== 第3步：建立HTTP连接 ==========
                URL url = new URL(MODIFY_CODE_WITH_DIFF_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法为POST
                connection.setRequestMethod("POST");

                // 设置请求头
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept-Charset", "UTF-8");

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

                        CodeDiffResult diffResult = parseDiffResponse(responseBody);

                        if (diffResult != null) {
                            // 成功，在UI线程调用成功回调
                            ApplicationManager.getApplication().invokeLater(() -> onSuccess.accept(diffResult));
                        } else {
                            // 解析失败或返回错误
                            String error = extractErrorFromDiffResponse(responseBody);
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
                ApplicationManager.getApplication().invokeLater(() -> onError.accept("连接失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 解析差异比较的JSON响应
     */
    private CodeDiffResult parseDiffResponse(String jsonResponse) {
        try {
            // 这是一个简化的JSON解析实现
            // 在实际项目中，应该使用Gson或Jackson等JSON库

            CodeDiffResult result = new CodeDiffResult();

            // 解析原始代码
            String originalCode = extractStringValue(jsonResponse, "originalCode");
            result.setOriginalCode(originalCode);

            // 解析修改后代码
            String modifiedCode = extractStringValue(jsonResponse, "modifiedCode");
            result.setModifiedCode(modifiedCode);

            // 调试信息：打印解析后的代码
            System.out.println("=== 解析后的代码调试信息 ===");
            System.out.println("原始代码长度: " + (originalCode != null ? originalCode.length() : "null"));
            if (originalCode != null && originalCode.length() > 0) {
                System.out.println("原始代码前50个字符: " + originalCode.substring(0, Math.min(50, originalCode.length())));
            }
            System.out.println("修改后代码长度: " + (modifiedCode != null ? modifiedCode.length() : "null"));
            if (modifiedCode != null && modifiedCode.length() > 0) {
                System.out.println("修改后代码前50个字符: " + modifiedCode.substring(0, Math.min(50, modifiedCode.length())));
            }
            System.out.println("========================");

            // 解析状态
            String status = extractStringValue(jsonResponse, "status");
            if ("error".equals(status)) {
                String error = extractStringValue(jsonResponse, "error");
                result.setError(error);
                return result;
            }

            // 解析指令
            String instruction = extractStringValue(jsonResponse, "instruction");
            result.setInstruction(instruction);

            // 解析文件名
            String fileName = extractStringValue(jsonResponse, "fileName");
            result.setFileName(fileName);

            // 解析差异块（简化实现，实际应该更复杂）
            // 这里先返回基本的结果，差异块解析可以后续优化

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从JSON响应中提取字符串值
     */
    private String extractStringValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) {
                return "";
            }

            startIndex += searchKey.length();

            // 查找结束引号（需要考虑转义字符）
            StringBuilder result = new StringBuilder();
            boolean escaped = false;
            for (int i = startIndex; i < json.length(); i++) {
                char c = json.charAt(i);

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
                        case 'u':
                            // 处理Unicode转义字符，如 \u4e2d\u6587
                            if (i + 4 < json.length()) {
                                String hexStr = json.substring(i + 1, i + 5);
                                try {
                                    int unicodeValue = Integer.parseInt(hexStr, 16);
                                    result.append((char) unicodeValue);
                                    i += 4; // 跳过4位十六进制数
                                } catch (NumberFormatException e) {
                                    // 如果解析失败，保持原样
                                    result.append(c);
                                }
                            } else {
                                result.append(c);
                            }
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
            return "";
        }
    }

    /**
     * 从差异比较的JSON响应中提取错误信息
     */
    private String extractErrorFromDiffResponse(String jsonResponse) {
        try {
            String error = extractStringValue(jsonResponse, "error");
            return error.isEmpty() ? "未知错误" : error;
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
