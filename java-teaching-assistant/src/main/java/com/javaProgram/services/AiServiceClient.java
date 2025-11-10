package com.javaProgram.services;

import com.intellij.openapi.application.ApplicationManager;
import com.javaProgram.utils.HttpUtil;
import com.javaProgram.utils.JsonUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * AI服务客户端（重构版）
 * 使用统一的HTTP和JSON工具类，消除代码重复
 */
public class AiServiceClient {
    // ============ 后端服务器配置 ============
    // 切换本地开发和远程服务器：将 USE_REMOTE_SERVER 设置为 true 使用远程服务器
    private static final boolean USE_REMOTE_SERVER = true; // true: 使用远程服务器, false: 使用本地服务器

    private static final String LOCAL_SERVER = "http://localhost:8081";
    private static final String REMOTE_SERVER = "http://your-serivce-ip:8081";
    private static final String SERVER_BASE = USE_REMOTE_SERVER ? REMOTE_SERVER : LOCAL_SERVER;

    private static final String BASE_URL = SERVER_BASE + "/api/ai/chat";
    private static final String MODIFY_CODE_URL = SERVER_BASE + "/api/ai/modify-code";
    private static final String MODIFY_CODE_WITH_DIFF_URL = SERVER_BASE + "/api/ai/modify-code-with-diff";
    private static final String DETECT_INTENT_URL = SERVER_BASE + "/api/ai/detect-intent";

    private int memoryId;

    public AiServiceClient(int memoryId) {
        this.memoryId = memoryId;
    }

    /**
     * 发送聊天消息并流式接收响应
     *
     * @param message     用户消息
     * @param projectPath 项目根目录路径（可选，用于AI自主读取代码）
     * @param onChunk     接收到数据块时的回调
     * @param onComplete  完成时的回调
     * @param onError     出错时的回调
     */
    public void sendMessage(String message, String projectPath, Consumer<String> onChunk,
            Runnable onComplete, Consumer<String> onError) {

        // 在后台线程执行网络请求
        executeInBackground(() -> {
            try {
                // 构建请求配置
                HttpRequestConfig config = buildChatRequestConfig(message, projectPath);

                // 执行流式请求
                HttpUtil.executeStreamingRequest(
                        config,
                        // onChunk - 在UI线程回调
                        chunk -> runOnUiThread(() -> onChunk.accept(chunk)),
                        // onComplete - 在UI线程回调
                        () -> runOnUiThread(onComplete),
                        // onError - 在UI线程回调
                        error -> runOnUiThread(() -> onError.accept(error)));
            } catch (Exception e) {
                runOnUiThread(() -> onError.accept("请求失败: " + e.getMessage()));
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

        executeInBackground(() -> {
            try {
                // 构建请求配置
                HttpRequestConfig config = buildModifyCodeRequestConfig(
                        originalCode, instruction, fileName);

                // 执行请求
                HttpUtil.executeRequest(
                        config,
                        // onSuccess - 解析响应并在UI线程回调
                        response -> {
                            if (JsonUtil.isErrorResponse(response)) {
                                String error = JsonUtil.extractError(response);
                                runOnUiThread(() -> onError.accept(error));
                            } else {
                                String modifiedCode = JsonUtil.extractStringValue(response, "modifiedCode");
                                if (modifiedCode != null && !modifiedCode.isEmpty()) {
                                    runOnUiThread(() -> onSuccess.accept(modifiedCode));
                                } else {
                                    runOnUiThread(() -> onError.accept("未能解析修改后的代码"));
                                }
                            }
                        },
                        // onError - 在UI线程回调
                        error -> runOnUiThread(() -> onError.accept(error)));
            } catch (Exception e) {
                runOnUiThread(() -> onError.accept("请求失败: " + e.getMessage()));
            }
        });
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

        executeInBackground(() -> {
            try {
                // 构建请求配置
                HttpRequestConfig config = new HttpRequestConfig.Builder()
                        .url(MODIFY_CODE_WITH_DIFF_URL)
                        .method("POST")
                        .contentType("application/json; charset=UTF-8")
                        .requestBody(buildModifyCodeRequestBody(originalCode, instruction, fileName))
                        .build();

                // 执行请求
                HttpUtil.executeRequest(
                        config,
                        // onSuccess - 解析响应并在UI线程回调
                        response -> {
                            System.out.println("=== 收到后端响应 ===");
                            System.out.println("响应长度: " + response.length() + " 字符");
                            System.out.println("响应前500字符: " + response.substring(0, Math.min(500, response.length())));
                            System.out.println("==================");

                            CodeDiffResult result = parseDiffResponse(response);
                            if (result != null && result.getError() == null) {
                                System.out.println("✓ 解析成功！");
                                System.out.println("  原始代码长度: "
                                        + (result.getOriginalCode() != null ? result.getOriginalCode().length() : 0));
                                System.out.println("  修改代码长度: "
                                        + (result.getModifiedCode() != null ? result.getModifiedCode().length() : 0));
                                runOnUiThread(() -> onSuccess.accept(result));
                            } else {
                                String error = result != null ? result.getError() : "解析响应失败";
                                System.err.println("✗ 解析失败！错误: " + error);
                                runOnUiThread(() -> onError.accept(error));
                            }
                        },
                        // onError - 在UI线程回调
                        error -> {
                            System.err.println("=== HTTP请求失败 ===");
                            System.err.println("错误: " + error);
                            System.err.println("==================");
                            runOnUiThread(() -> onError.accept(error));
                        });
            } catch (Exception e) {
                runOnUiThread(() -> onError.accept("请求失败: " + e.getMessage()));
            }
        });
    }

    /**
     * AI意图识别
     * 判断用户消息是修改代码意图还是普通对话意图
     *
     * @param message   用户消息
     * @param onSuccess 成功回调，接收意图类型 "modify" 或 "chat"
     * @param onError   失败回调，接收错误信息
     */
    public void detectIntent(String message, Consumer<String> onSuccess, Consumer<String> onError) {
        executeInBackground(() -> {
            try {
                // 构建请求配置
                HttpRequestConfig config = new HttpRequestConfig.Builder()
                        .url(DETECT_INTENT_URL)
                        .method("POST")
                        .contentType("application/json; charset=UTF-8")
                        .requestBody("{\"message\":\"" + JsonUtil.escapeJson(message) + "\"}")
                        .build();

                // 执行请求
                HttpUtil.executeRequest(
                        config,
                        // onSuccess - 解析响应并在UI线程回调
                        response -> {
                            String intent = JsonUtil.extractStringValue(response, "intent");
                            if (intent != null && (intent.equals("modify") || intent.equals("chat"))) {
                                runOnUiThread(() -> onSuccess.accept(intent));
                            } else {
                                // 如果解析失败，默认为chat
                                runOnUiThread(() -> onSuccess.accept("chat"));
                            }
                        },
                        // onError - 在UI线程回调，默认为chat
                        error -> {
                            System.err.println("意图识别失败，默认为chat: " + error);
                            runOnUiThread(() -> onSuccess.accept("chat")); // 出错时默认为chat，不影响用户体验
                        });
            } catch (Exception e) {
                System.err.println("意图识别异常，默认为chat: " + e.getMessage());
                runOnUiThread(() -> onSuccess.accept("chat")); // 异常时默认为chat
            }
        });
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建聊天请求配置
     */
    private HttpRequestConfig buildChatRequestConfig(String message, String projectPath) {
        try {
            // 构建URL查询参数
            String url = BASE_URL + "?memoryId=" + memoryId;

            // 构建请求体（form-urlencoded格式）
            StringBuilder requestBody = new StringBuilder();
            requestBody.append("message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));

            if (projectPath != null && !projectPath.trim().isEmpty()) {
                requestBody.append("&projectPath=")
                        .append(URLEncoder.encode(projectPath, StandardCharsets.UTF_8));
            }

            return new HttpRequestConfig.Builder()
                    .url(url)
                    .method("POST")
                    .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                    .requestBody(requestBody.toString())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("构建请求配置失败", e);
        }
    }

    /**
     * 构建代码修改请求配置
     */
    private HttpRequestConfig buildModifyCodeRequestConfig(
            String originalCode, String instruction, String fileName) {

        return new HttpRequestConfig.Builder()
                .url(MODIFY_CODE_URL)
                .method("POST")
                .contentType("application/json; charset=UTF-8")
                .requestBody(buildModifyCodeRequestBody(originalCode, instruction, fileName))
                .build();
    }

    /**
     * 构建代码修改请求体（JSON格式）
     */
    private String buildModifyCodeRequestBody(String originalCode, String instruction, String fileName) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"originalCode\":\"").append(JsonUtil.escapeJson(originalCode)).append("\",");
        json.append("\"instruction\":\"").append(JsonUtil.escapeJson(instruction)).append("\"");

        if (fileName != null && !fileName.trim().isEmpty()) {
            json.append(",\"fileName\":\"").append(JsonUtil.escapeJson(fileName)).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 解析差异比较响应
     */
    private CodeDiffResult parseDiffResponse(String jsonResponse) {
        try {
            CodeDiffResult result = new CodeDiffResult();

            // 检查是否有错误
            if (JsonUtil.isErrorResponse(jsonResponse)) {
                result.setError(JsonUtil.extractError(jsonResponse));
                return result;
            }

            // 提取字段
            result.setOriginalCode(JsonUtil.extractStringValue(jsonResponse, "originalCode"));
            result.setModifiedCode(JsonUtil.extractStringValue(jsonResponse, "modifiedCode"));
            result.setInstruction(JsonUtil.extractStringValue(jsonResponse, "instruction"));
            result.setFileName(JsonUtil.extractStringValue(jsonResponse, "fileName"));

            // 验证必要字段
            if ((result.getOriginalCode() == null || result.getOriginalCode().isEmpty()) &&
                    (result.getModifiedCode() == null || result.getModifiedCode().isEmpty())) {
                result.setError("后端返回的代码为空。原始响应长度: " + jsonResponse.length() + "字符");
                System.err.println("=== 解析失败的JSON响应 ===");
                System.err.println(jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
                System.err.println("======================");
            }

            return result;
        } catch (Exception e) {
            CodeDiffResult result = new CodeDiffResult();
            result.setError("解析响应失败: " + e.getMessage() + "\n响应长度: " + jsonResponse.length() + "字符");
            System.err.println("=== JSON解析异常 ===");
            e.printStackTrace();
            System.err.println("响应前500字符: " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
            System.err.println("===================");
            return result;
        }
    }

    /**
     * 在后台线程执行任务
     */
    private void executeInBackground(Runnable task) {
        ApplicationManager.getApplication().executeOnPooledThread(task);
    }

    /**
     * 在UI线程执行任务
     */
    private void runOnUiThread(Runnable task) {
        ApplicationManager.getApplication().invokeLater(task);
    }

    // ==================== Getter & Setter ====================

    public void setMemoryId(int memoryId) {
        this.memoryId = memoryId;
    }

    public int getMemoryId() {
        return memoryId;
    }
}
