package com.example.aicodehelper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiCodeHelper {

    @Resource
    private ChatModel qwenChatModel;

    private static final String SYSTEM_MESSAGE = """
    你是《Java企业级应用开发》课程的助教。

    **最重要的规则：每个回答的第一行必须说明信息来源！**

    格式要求：
        - 如果找到课程资料：📚 **信息来源：《文档名》第X页**
        - 如果未找到资料：💡 **本回答基于通用知识，未引用特定课程资料**

    绝不等待用户询问来源，必须主动说明。

    现在请按此规则回答学生的问题。
    """;
    // 简单对话
    public String chat(String message){
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse chatResponse = qwenChatModel.chat(systemMessage,userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI 输出："+aiMessage.toString());
        return aiMessage.text();
    }

    // 自定义用户消息
    public String chatWithMessage(UserMessage userMessage){
        ChatResponse chatResponse = qwenChatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI 输出："+aiMessage.toString());
        return aiMessage.text();
    }
}
