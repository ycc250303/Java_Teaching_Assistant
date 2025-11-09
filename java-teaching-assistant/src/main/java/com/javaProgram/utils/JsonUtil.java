package com.javaProgram.utils;

/**
 * JSON工具类
 * 提供简单的JSON解析和构建功能（不依赖第三方库）
 */
public class JsonUtil {

    /**
     * 转义JSON字符串中的特殊字符
     */
    public static String escapeJson(String str) {
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
     * 从JSON字符串中提取指定字段的值
     *
     * @param json JSON字符串
     * @param key  字段名
     * @return 字段值，如果不存在则返回空字符串
     */
    public static String extractStringValue(String json, String key) {
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
                    String unescaped = unescapeChar(c, json, i);
                    result.append(unescaped);
                    // Unicode转义需要跳过额外字符（格式：反斜杠u加4个十六进制数字）
                    if (c == 'u' && i + 4 < json.length()) {
                        i += 4; // 跳过4个十六进制字符
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
     * 反转义单个字符
     */
    private static String unescapeChar(char c, String json, int position) {
        switch (c) {
            case 'n':
                return "\n";
            case 'r':
                return "\r";
            case 't':
                return "\t";
            case '\\':
                return "\\";
            case '"':
                return "\"";
            case 'u':
                // 处理Unicode转义字符，如 \u4e2d\u6587
                if (position + 4 < json.length()) {
                    String hexStr = json.substring(position + 1, position + 5);
                    try {
                        int unicodeValue = Integer.parseInt(hexStr, 16);
                        return String.valueOf((char) unicodeValue);
                    } catch (NumberFormatException e) {
                        return String.valueOf(c);
                    }
                }
                return String.valueOf(c);
            default:
                return String.valueOf(c);
        }
    }

    /**
     * 构建简单的JSON对象
     *
     * @param fields 字段键值对（key1, value1, key2, value2, ...）
     * @return JSON字符串
     */
    public static String buildJsonObject(String... fields) {
        if (fields.length % 2 != 0) {
            throw new IllegalArgumentException("字段参数必须成对出现");
        }

        StringBuilder json = new StringBuilder("{");

        for (int i = 0; i < fields.length; i += 2) {
            if (i > 0) {
                json.append(",");
            }
            String key = fields[i];
            String value = fields[i + 1];

            json.append("\"").append(key).append("\":\"")
                    .append(escapeJson(value)).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 检查JSON响应是否表示错误
     */
    public static boolean isErrorResponse(String json) {
        // 只有当 status 明确为 "error" 时才认为是错误响应
        if (json.contains("\"status\":\"error\"")) {
            return true;
        }

        // 检查 error 字段是否有实际内容（不是 null 或空字符串）
        String errorValue = extractStringValue(json, "error");
        return errorValue != null && !errorValue.trim().isEmpty() && !"null".equals(errorValue);
    }

    /**
     * 从错误响应中提取错误信息
     */
    public static String extractError(String json) {
        String error = extractStringValue(json, "error");
        return (error == null || error.isEmpty()) ? "未知错误" : error;
    }
}
