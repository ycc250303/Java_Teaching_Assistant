package com.example.aicodehelper.controller;

import com.example.aicodehelper.ai.AiCodeHelperService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * 基于RAG的聊天接口（强制标注来源）
     */
    @GetMapping("/chat-with-rag")
    public String chatWithRag(String message) {
        try {
            // 在用户消息前添加强制标注来源的指令
            String enhancedMessage = "请先说明信息来源，然后回答：" + message;
            String response = aiCodeHelperService.chatWithRag(enhancedMessage).content();

            // 确保回答包含来源标注
            if (!response.contains("信息来源") && !response.contains("基于企业级开发经验")) {
                return "💡 **基于企业级开发经验回答**\n\n" + response;
            }

            return response;
        } catch (Exception e) {
            return "抱歉，处理您的请求时遇到了问题：" + e.getMessage();
        }
    }
}