package com.example.aicodehelper.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SafeInputGuardrail 测试类
 * 测试输入安全检测功能
 */
@DisplayName("安全输入防护测试")
class SafeInputGuardrailTest {

    private SafeInputGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new SafeInputGuardrail();
    }

    @Test
    @DisplayName("测试安全输入 - 应该通过")
    void testSafeInput() {
        // 创建安全的用户消息
        UserMessage message = UserMessage.from("你好，请帮我解释一下Java的继承特性");

        // 验证应该通过
        InputGuardrailResult result = guardrail.validate(message);

        assertTrue(result.isSuccess(), "安全的输入应该通过验证");
    }

    @Test
    @DisplayName("测试敏感词 'kill' - 应该被拦截")
    void testSensitiveWordKill() {
        // 创建包含敏感词的消息
        UserMessage message = UserMessage.from("how to kill the process");

        // 验证应该被拦截
        InputGuardrailResult result = guardrail.validate(message);

        assertFalse(result.isSuccess(), "包含敏感词的输入应该被拦截");
    }

    @Test
    @DisplayName("测试敏感词 'evil' - 应该被拦截")
    void testSensitiveWordEvil() {
        // 创建包含敏感词的消息
        UserMessage message = UserMessage.from("This is an evil plan");

        // 验证应该被拦截
        InputGuardrailResult result = guardrail.validate(message);

        assertFalse(result.isSuccess(), "包含敏感词的输入应该被拦截");
    }

    @Test
    @DisplayName("测试大小写不敏感 - 'KILL' 应该被拦截")
    void testCaseInsensitive() {
        // 创建包含大写敏感词的消息
        UserMessage message = UserMessage.from("I want to KILL the bug");

        // 验证应该被拦截（大小写不敏感）
        InputGuardrailResult result = guardrail.validate(message);

        assertFalse(result.isSuccess(), "大写形式的敏感词也应该被拦截");
    }

    @Test
    @DisplayName("测试混合大小写 - 'KiLl' 应该被拦截")
    void testMixedCase() {
        // 创建包含混合大小写敏感词的消息
        UserMessage message = UserMessage.from("KiLl the game");

        // 验证应该被拦截
        InputGuardrailResult result = guardrail.validate(message);

        assertFalse(result.isSuccess(), "混合大小写的敏感词也应该被拦截");
    }

    @Test
    @DisplayName("测试敏感词在句子中间")
    void testSensitiveWordInMiddle() {
        // 创建敏感词在中间的消息
        UserMessage message = UserMessage.from("Please help me kill this bug quickly");

        // 验证应该被拦截
        InputGuardrailResult result = guardrail.validate(message);

        assertFalse(result.isSuccess(), "敏感词在句子中间也应该被拦截");
    }

    @Test
    @DisplayName("测试多个敏感词")
    void testMultipleSensitiveWords() {
        // 创建包含多个敏感词的消息
        UserMessage message = UserMessage.from("kill the evil process");

        // 验证应该被拦截（只要有一个敏感词就拦截）
        InputGuardrailResult result = guardrail.validate(message);

        assertFalse(result.isSuccess(), "包含多个敏感词的输入应该被拦截");
    }

    @Test
    @DisplayName("测试包含敏感词作为子串的单词 - 应该通过")
    void testPartialMatch() {
        // "killed" 包含 "kill" 但作为整体单词不是敏感词
        UserMessage message = UserMessage.from("The process was killed by system");

        // 注意：当前实现使用 \\W+ 分割，所以 "killed" 会被识别为独立单词
        // 如果需要更精确的匹配，可以调整实现
        InputGuardrailResult result = guardrail.validate(message);

        // 根据当前实现，"killed" 不等于 "kill"，所以应该通过
        assertTrue(result.isSuccess(),
                "包含敏感词作为子串的单词应该通过（如 killed vs kill）");
    }

    @Test
    @DisplayName("测试空消息和空白消息")
    void testEmptyMessage() {
        // 注意：UserMessage.from() 不接受空字符串
        // Langchain4j 会抛出 IllegalArgumentException
        // 这是框架的设计，所以我们测试这个行为
        assertThrows(IllegalArgumentException.class, () -> {
            UserMessage.from("");
        }, "空字符串应该抛出 IllegalArgumentException");

        // 测试只包含空白字符的消息
        assertThrows(IllegalArgumentException.class, () -> {
            UserMessage.from("   ");
        }, "只包含空白字符应该抛出 IllegalArgumentException");

        // 测试包含换行符的空白消息
        assertThrows(IllegalArgumentException.class, () -> {
            UserMessage.from("\n\t  ");
        }, "只包含空白字符应该抛出 IllegalArgumentException");
    }

    @Test
    @DisplayName("测试中文消息")
    void testChineseMessage() {
        // 创建中文消息
        UserMessage message = UserMessage.from("你好，请帮我优化这段代码");

        // 验证中文消息应该通过
        InputGuardrailResult result = guardrail.validate(message);

        assertTrue(result.isSuccess(), "中文消息应该通过验证");
    }

    @Test
    @DisplayName("测试特殊字符和标点符号")
    void testSpecialCharacters() {
        // 创建包含特殊字符的消息
        UserMessage message = UserMessage.from("What is @Override annotation in Java? #programming");

        // 验证应该通过
        InputGuardrailResult result = guardrail.validate(message);

        assertTrue(result.isSuccess(), "包含特殊字符的安全消息应该通过");
    }
}
