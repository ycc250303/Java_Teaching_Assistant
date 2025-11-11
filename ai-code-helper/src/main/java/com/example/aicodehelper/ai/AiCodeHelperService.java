package com.example.aicodehelper.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiCodeHelperService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String message);

    record Report(String name, List<String> suggestionList) {
    };

    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String message);

    // 流式对话
    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);

    /**
     * 代码修改方法
     * 接收包含完整提示词的消息，返回修改后的代码
     * 不使用标准的system-prompt，因为代码修改需要直接返回代码，不需要信息来源标注
     */
    @SystemMessage("你是一个专业的Java代码助手。用户会给你原始代码和修改指令，你需要直接返回修改后的完整代码。只返回代码，不要添加任何解释、说明或标注。代码必须保持原有格式和缩进。如果代码被markdown代码块包裹，请去掉包裹标记。")
    @UserMessage("{{prompt}}")
    String modifyCode(String prompt);

    /**
     * 意图识别方法
     * 判断用户的消息是否表达了修改代码的意图
     * 返回 "modify" 表示修改意图，"chat" 表示普通对话意图
     */
    @SystemMessage("你是一个意图识别专家。用户会发送一条消息，你需要判断用户的意图是'修改代码'还是'普通对话'。\n\n" +
            "修改代码意图的特征：\n" +
            "- 要求修改、优化、重构、改进现有代码\n" +
            "- 要求添加、删除、修复代码中的某些部分\n" +
            "- 要求改变代码的实现方式或逻辑\n" +
            "- 要求让代码性能更好、更简洁、更安全\n" +
            "- 明确表示要对代码进行操作性变更\n\n" +
            "普通对话意图的特征：\n" +
            "- 询问代码的解释、原理、为什么这样写\n" +
            "- 请求学习建议、最佳实践、概念解释\n" +
            "- 讨论技术方案、比较不同方法的优缺点\n" +
            "- 仅仅是咨询而不涉及具体的代码修改\n\n" +
            "请仔细分析用户的意图，只返回一个单词：\n" +
            "- 返回 'modify' 表示用户想要修改代码\n" +
            "- 返回 'chat' 表示用户想要普通对话\n\n" +
            "只返回这个单词，不要返回其他任何内容。")
    @UserMessage("用户消息：{{message}}")
    String detectIntent(String message);
}
