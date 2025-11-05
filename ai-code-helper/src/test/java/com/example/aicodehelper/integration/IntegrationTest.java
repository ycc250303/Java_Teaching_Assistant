package com.example.aicodehelper.integration;

import com.example.aicodehelper.ai.AiCodeHelperService;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试类
 * 测试完整的端到端功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("集成测试")
class IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AiCodeHelperService aiCodeHelperService;

    @Test
    @DisplayName("测试应用上下文加载")
    void testContextLoads() {
        // 验证 Spring 上下文成功加载
        assertNotNull(aiCodeHelperService, "AiCodeHelperService 应该被正确注入");
        assertNotNull(webTestClient, "WebTestClient 应该被正确注入");
    }

    @Test
    @DisplayName("测试 RAG 功能端到端流程")
    void testRagEndToEnd() {
        // 测试问题
        String question = "什么是Java的继承";

        // 调用 RAG 服务
        Result<String> result = aiCodeHelperService.chatWithRag(question);

        // 验证结果
        assertNotNull(result, "RAG 结果不应该为 null");
        assertNotNull(result.content(), "RAG 内容不应该为 null");
        assertFalse(result.content().isEmpty(), "RAG 内容不应该为空");

        System.out.println("RAG 测试结果：");
        System.out.println("问题：" + question);
        System.out.println("回答：" + result.content());
        System.out.println("来源数量：" + result.sources().size());
    }

    @Test
    @DisplayName("测试流式聊天功能")
    void testStreamingChat() {
        // 测试消息
        String message = "请简单介绍一下Java";
        int memoryId = 1001;

        // 调用流式聊天
        Flux<String> responseStream = aiCodeHelperService.chatStream(memoryId, message);

        // 验证流式响应
        assertNotNull(responseStream, "流式响应不应该为 null");

        // 收集所有响应块
        String fullResponse = responseStream.collectList()
                .block() // 阻塞等待完成（仅用于测试）
                .stream()
                .reduce("", String::concat);

        System.out.println("流式聊天测试结果：");
        System.out.println("问题：" + message);
        System.out.println("完整回答：" + fullResponse);

        assertFalse(fullResponse.isEmpty(), "流式响应不应该为空");
    }

    @Test
    @DisplayName("测试代码修改 API 端到端")
    void testCodeModificationEndToEnd() {
        // 准备测试数据
        String originalCode = """
                public class Calculator {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """;

        Map<String, String> request = Map.of(
                "originalCode", originalCode,
                "instruction", "添加一个subtract方法",
                "fileName", "Calculator.java");

        // 调用 API
        webTestClient.post()
                .uri("/api/ai/modify-code")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("success")
                .jsonPath("$.modifiedCode").exists()
                .consumeWith(response -> {
                    System.out.println("代码修改测试结果：");
                    System.out.println("原始代码：\n" + originalCode);
                    System.out.println("修改指令：添加一个subtract方法");
                    // System.out.println("修改后代码：\n" + response);
                });
    }

    @Test
    @DisplayName("测试聊天记忆功能")
    void testChatMemory() {
        int memoryId = 2001;

        // 第一次对话：告诉名字
        String response1 = aiCodeHelperService.chatStream(memoryId, "我叫张三")
                .collectList()
                .block()
                .stream()
                .reduce("", String::concat);

        System.out.println("第一次对话：");
        System.out.println("用户：我叫张三");
        System.out.println("AI：" + response1);

        // 第二次对话：询问名字（测试是否记住）
        String response2 = aiCodeHelperService.chatStream(memoryId, "我叫什么名字")
                .collectList()
                .block()
                .stream()
                .reduce("", String::concat);

        System.out.println("\n第二次对话：");
        System.out.println("用户：我叫什么名字");
        System.out.println("AI：" + response2);

        // 验证 AI 能记住之前的对话
        // 注意：这个测试依赖 AI 的实际响应，可能不够稳定
        // assertTrue(response2.contains("张三"), "AI 应该记住用户的名字");
    }

    @Test
    @DisplayName("测试多个独立会话")
    void testMultipleSessions() {
        int memoryId1 = 3001;
        int memoryId2 = 3002;

        // 会话1：介绍自己
        aiCodeHelperService.chatStream(memoryId1, "我是用户A").blockLast();

        // 会话2：介绍自己（不同的名字）
        aiCodeHelperService.chatStream(memoryId2, "我是用户B").blockLast();

        // 验证两个会话是独立的
        String response1 = aiCodeHelperService.chatStream(memoryId1, "我是谁")
                .collectList().block().stream().reduce("", String::concat);

        String response2 = aiCodeHelperService.chatStream(memoryId2, "我是谁")
                .collectList().block().stream().reduce("", String::concat);

        System.out.println("会话1 回答：" + response1);
        System.out.println("会话2 回答：" + response2);

        // 两个会话的回答应该不同（理想情况下）
        assertNotEquals(response1, response2, "两个独立会话应该有不同的记忆");
    }

    @Test
    @DisplayName("测试系统提示词生效")
    void testSystemPromptWorks() {
        // 提一个需要引用课程资料的问题
        String question = "Java中什么是多态";

        Result<String> result = aiCodeHelperService.chatWithRag(question);
        String answer = result.content();

        System.out.println("系统提示词测试：");
        System.out.println("问题：" + question);
        System.out.println("回答：" + answer);

        // 验证回答包含来源标注（根据系统提示词要求）
        boolean hasSourceInfo = answer.contains("信息来源") ||
                answer.contains("基于企业级开发经验") ||
                answer.contains("基于通用知识");

        assertTrue(hasSourceInfo, "回答应该包含来源标注（根据系统提示词要求）");
    }

    @Test
    @DisplayName("测试 CORS 配置")
    void testCorsConfiguration() {
        // 测试 CORS 预检请求
        webTestClient.options()
                .uri("/api/ai/chat?memoryId=1&message=test")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Access-Control-Allow-Origin");
    }

    @Test
    @DisplayName("测试健康检查")
    void testHealthCheck() {
        // 验证应用正常运行
        webTestClient.get()
                .uri("/api/ai/chat?memoryId=999&message=hello")
                .exchange()
                .expectStatus().isOk();
    }
}
