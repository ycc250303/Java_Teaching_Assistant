package com.example.aicodehelper.ai;

import com.example.aicodehelper.ai.tools.InterviewQuestionTool;
import com.example.aicodehelper.ai.tools.FileReaderTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    // 文件读取工具Bean
    @Bean
    public FileReaderTool fileReaderTool() {
        // 初始化时使用默认路径，后续会动态更新
        return new FileReaderTool(".");
    }

    @Bean
    public AiCodeHelperService aiCodeHelperService() {
        // 会话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        // 构造 AI 服务并直接返回
        return AiServices.builder(AiCodeHelperService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(qwenStreamingChatModel) // 流式输出
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10)) // 独立存储会话
                .contentRetriever(contentRetriever) // RAG 检索增强生成
                .tools(new InterviewQuestionTool(), fileReaderTool())// 工具调用
                .build();
    }
}