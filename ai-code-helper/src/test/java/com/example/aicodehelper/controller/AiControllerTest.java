package com.example.aicodehelper.controller;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * AiController 测试类
 * 测试所有 API 接口的基本功能和可访问性
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("AI 控制器测试")
class AiControllerTest {

    @Resource
    private WebTestClient webTestClient;

    @Test
    @DisplayName("测试流式聊天接口可访问性")
    void testChatStreamEndpoint() {
        // 测试端点是否可访问
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ai/chat")
                        .queryParam("memoryId", 123)
                        .queryParam("message", "你好")
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    @DisplayName("测试代码修改接口 - 缺少原始代码")
    void testModifyCodeMissingOriginalCode() {
        // 准备缺少原始代码的请求
        Map<String, String> request = Map.of(
                "instruction", "添加注释",
                "fileName", "Example.java");

        // 测试应该返回错误
        webTestClient.post()
                .uri("/api/ai/modify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @Test
    @DisplayName("测试代码修改接口 - 缺少修改指令")
    void testModifyCodeMissingInstruction() {
        // 准备缺少修改指令的请求
        Map<String, String> request = Map.of(
                "originalCode", "public class Example {}",
                "fileName", "Example.java");

        // 测试应该返回错误
        webTestClient.post()
                .uri("/api/ai/modify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error").exists();
    }

    @Test
    @DisplayName("测试代码修改接口 - 完整请求")
    void testModifyCodeCompleteRequest() {
        // 准备完整的请求体
        Map<String, String> request = Map.of(
                "originalCode", "public class Example {}",
                "instruction", "添加一个hello方法",
                "fileName", "Example.java");

        // 测试代码修改接口
        webTestClient.post()
                .uri("/api/ai/modify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }
}
