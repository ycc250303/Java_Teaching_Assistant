package com.example.aicodehelper.ai.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InterviewQuestionTool 测试类
 * 测试面试题搜索功能
 */
@DisplayName("面试题工具测试")
class InterviewQuestionToolTest {

    private InterviewQuestionTool tool;

    @BeforeEach
    void setUp() {
        tool = new InterviewQuestionTool();
    }

    @Test
    @DisplayName("测试搜索 Java 面试题")
    @Timeout(10) // 网络请求设置超时时间
    void testSearchJavaQuestions() {
        // 搜索 Java 相关面试题
        String result = tool.searchInterviewQuestions("Java");

        // 验证结果
        assertNotNull(result, "搜索结果不应该为 null");
        assertFalse(result.isEmpty(), "搜索结果不应该为空");

        System.out.println("Java 面试题搜索结果：");
        System.out.println(result);
        System.out.println("共找到 " + result.split("\n").length + " 个问题");
    }

    @Test
    @DisplayName("测试搜索 Redis 面试题")
    @Timeout(10)
    void testSearchRedisQuestions() {
        // 搜索 Redis 相关面试题
        String result = tool.searchInterviewQuestions("Redis");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        System.out.println("Redis 面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索中文关键词")
    @Timeout(10)
    void testSearchChineseKeyword() {
        // 搜索中文关键词
        String result = tool.searchInterviewQuestions("多线程");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        System.out.println("多线程面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索 MySQL 面试题")
    @Timeout(10)
    void testSearchMySQLQuestions() {
        // 搜索 MySQL 相关面试题
        String result = tool.searchInterviewQuestions("MySQL");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        System.out.println("MySQL 面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索 Spring 面试题")
    @Timeout(10)
    void testSearchSpringQuestions() {
        // 搜索 Spring 相关面试题
        String result = tool.searchInterviewQuestions("Spring");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        System.out.println("Spring 面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索可能不存在的关键词")
    @Timeout(10)
    void testSearchNonExistentKeyword() {
        // 搜索一个不太可能存在的关键词
        String result = tool.searchInterviewQuestions("xyz123abc456");

        // 验证结果（可能为空或返回相关推荐）
        assertNotNull(result);

        System.out.println("不存在关键词的搜索结果：");
        System.out.println(result.isEmpty() ? "无结果" : result);
    }

    @Test
    @DisplayName("测试搜索空关键词")
    @Timeout(10)
    void testSearchEmptyKeyword() {
        // 搜索空关键词
        String result = tool.searchInterviewQuestions("");

        // 验证结果
        assertNotNull(result);

        System.out.println("空关键词的搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索包含特殊字符的关键词")
    @Timeout(10)
    void testSearchSpecialCharacters() {
        // 搜索包含特殊字符的关键词
        String result = tool.searchInterviewQuestions("C++");

        // 验证结果
        assertNotNull(result);

        System.out.println("C++ 面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索复合关键词")
    @Timeout(10)
    void testSearchCompoundKeyword() {
        // 搜索复合关键词
        String result = tool.searchInterviewQuestions("Java多线程");

        // 验证结果
        assertNotNull(result);

        System.out.println("Java多线程 面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索算法相关面试题")
    @Timeout(10)
    void testSearchAlgorithmQuestions() {
        // 搜索算法相关面试题
        String result = tool.searchInterviewQuestions("算法");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        System.out.println("算法面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试搜索操作系统面试题")
    @Timeout(10)
    void testSearchOSQuestions() {
        // 搜索操作系统相关面试题
        String result = tool.searchInterviewQuestions("操作系统");

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());

        System.out.println("操作系统面试题搜索结果：");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试 URL 编码功能")
    void testUrlEncoding() {
        // 这个测试验证中文关键词能正确编码
        // 实际执行时会调用 URLEncoder.encode()

        String keyword = "Java多线程";
        String result = tool.searchInterviewQuestions(keyword);

        // 验证不会因为编码问题而失败
        assertNotNull(result);

        System.out.println("URL编码测试（中文关键词）结果：");
        System.out.println("关键词: " + keyword);
        System.out.println("搜索结果: " + (result.isEmpty() ? "空" : "成功"));
    }
}
