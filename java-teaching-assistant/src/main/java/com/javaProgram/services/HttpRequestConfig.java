package com.javaProgram.services;

/**
 * HTTP请求配置类
 * 用于统一管理HTTP请求的各项参数
 */
public class HttpRequestConfig {
    private final String url;
    private final String method;
    private final String contentType;
    private final String requestBody;
    private final int connectTimeout;
    private final int readTimeout;

    private HttpRequestConfig(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.contentType = builder.contentType;
        this.requestBody = builder.requestBody;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getContentType() {
        return contentType;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Builder模式构建请求配置
     */
    public static class Builder {
        private String url;
        private String method = "POST";
        private String contentType = "application/json; charset=UTF-8";
        private String requestBody;
        private int connectTimeout = 5000;
        private int readTimeout = 120000;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder requestBody(String requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public HttpRequestConfig build() {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("URL不能为空");
            }
            return new HttpRequestConfig(this);
        }
    }
}
