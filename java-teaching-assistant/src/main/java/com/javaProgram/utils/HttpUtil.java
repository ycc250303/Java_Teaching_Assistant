package com.javaProgram.utils;

import com.javaProgram.services.HttpRequestConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * HTTP工具类
 * 提供统一的HTTP请求处理逻辑
 */
public class HttpUtil {

    /**
     * 执行HTTP请求并流式读取响应（用于SSE）
     *
     * @param config     请求配置
     * @param onChunk    接收到数据块时的回调
     * @param onComplete 完成时的回调
     * @param onError    出错时的回调
     */
    public static void executeStreamingRequest(
            HttpRequestConfig config,
            Consumer<String> onChunk,
            Runnable onComplete,
            Consumer<String> onError) {

        try {
            // 建立HTTP连接
            HttpURLConnection connection = createConnection(config);

            // 写入请求体
            if (config.getRequestBody() != null && !config.getRequestBody().isEmpty()) {
                writeRequestBody(connection, config.getRequestBody());
            }

            // 检查响应状态
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // 流式读取响应
                readStreamingResponse(connection, onChunk);
                onComplete.run();
            } else {
                onError.accept("HTTP Error: " + connection.getResponseCode());
            }

        } catch (Exception e) {
            onError.accept("连接失败: " + e.getMessage());
        }
    }

    /**
     * 执行HTTP请求并一次性读取完整响应
     *
     * @param config    请求配置
     * @param onSuccess 成功回调，接收响应内容
     * @param onError   失败回调，接收错误信息
     */
    public static void executeRequest(
            HttpRequestConfig config,
            Consumer<String> onSuccess,
            Consumer<String> onError) {

        try {
            // 建立HTTP连接
            HttpURLConnection connection = createConnection(config);

            // 写入请求体
            if (config.getRequestBody() != null && !config.getRequestBody().isEmpty()) {
                writeRequestBody(connection, config.getRequestBody());
            }

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取完整响应
                String response = readFullResponse(connection);
                onSuccess.accept(response);
            } else {
                // 读取错误响应
                String errorResponse = readErrorResponse(connection, responseCode);
                onError.accept(errorResponse);
            }

        } catch (Exception e) {
            onError.accept("连接失败: " + e.getMessage());
        }
    }

    /**
     * 创建HTTP连接
     */
    private static HttpURLConnection createConnection(HttpRequestConfig config) throws Exception {
        URL url = new URL(config.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方法
        connection.setRequestMethod(config.getMethod());

        // 设置请求头
        connection.setRequestProperty("Content-Type", config.getContentType());
        connection.setRequestProperty("Accept", config.getContentType());
        connection.setRequestProperty("Accept-Charset", "UTF-8");

        // 如果是POST/PUT，允许输出
        if ("POST".equals(config.getMethod()) || "PUT".equals(config.getMethod())) {
            connection.setDoOutput(true);
        }

        // 设置超时
        connection.setConnectTimeout(config.getConnectTimeout());
        connection.setReadTimeout(config.getReadTimeout());

        return connection;
    }

    /**
     * 写入请求体
     */
    private static void writeRequestBody(HttpURLConnection connection, String requestBody) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody);
            writer.flush();
        }
    }

    /**
     * 流式读取响应（SSE格式）
     */
    private static void readStreamingResponse(HttpURLConnection connection, Consumer<String> onChunk) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // SSE格式：data: xxx
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (!data.isEmpty()) {
                        onChunk.accept(data);
                    }
                }
            }
        }
    }

    /**
     * 读取完整响应
     */
    private static String readFullResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * 读取错误响应
     */
    private static String readErrorResponse(HttpURLConnection connection, int responseCode) {
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
        return error;
    }
}
