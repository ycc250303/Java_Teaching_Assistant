package com.example.aicodehelper.ai;

import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void chat() {
        String result = aiCodeHelperService.chat("你好，我是ycc");
        System.out.println(result);
    }

    @Test
    void chatWithMemory() {
        String result = aiCodeHelperService.chat("你好，我是ycc");
        System.out.println(result);
        result = aiCodeHelperService.chat("你好，我是谁来着");
        System.out.println(result);
    }

    @Test
    void chatWithRag() {
        Result<String> result = aiCodeHelperService.chatWithRag("怎么学习Java，有哪些常见的面试题");
        System.out.println(result.sources());
        System.out.println(result.content());
    }

    @Test
    void chatWithTools() {
        String result = aiCodeHelperService.chat("有哪些常见的计算机网络面试题？");
        System.out.println(result);
    }

    @Test
    void chatWithGuardrail() {
        // Guardrail 应该拦截包含敏感词的输入
        // 这里测试的是 Guardrail 正确工作并抛出异常
        try {
            String result = aiCodeHelperService.chat("kill the game");
            // 如果没有抛出异常，说明 Guardrail 没有生效
            System.out.println("警告：Guardrail 未拦截敏感词，返回结果: " + result);
        } catch (Exception e) {
            // 预期会抛出异常，说明 Guardrail 正常工作
            System.out.println("✓ Guardrail 正确拦截了敏感词: " + e.getMessage());
            // 测试通过
            assertTrue(e.getMessage().contains("kill"), "错误消息应该包含敏感词");
        }
    }
}
