package com.example.aicodehelper.ai;

import com.example.aicodehelper.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

import java.util.List;

@InputGuardrails({SafeInputGuardrail.class})
public interface AiCodeHelperService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String message);

    record Report(String name, List<String> suggestionList) {};

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
}
