package com.example.aicodehelper.controller;

import com.example.aicodehelper.ai.AiCodeHelperService;
import com.example.aicodehelper.ai.tools.FileReaderTool;
import com.example.aicodehelper.dto.CodeDiffResult;
import com.example.aicodehelper.util.DiffUtils;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private FileReaderTool fileReaderTool;

    /**
     * 聊天接口（支持GET和POST方法）
     * GET: 参数在URL中（短消息）
     * POST: 参数在请求体中（长消息，如带代码上下文）
     * 
     * @param memoryId    会话ID
     * @param message     用户消息
     * @param projectPath 项目根目录路径（可选，用于AI自主读取代码）
     */
    @RequestMapping(value = "/chat", method = { RequestMethod.GET, RequestMethod.POST })
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam int memoryId,
            @RequestParam String message,
            @RequestParam(required = false) String projectPath) {

        // 如果前端提供了项目路径，更新 FileReaderTool 的工作目录
        if (projectPath != null && !projectPath.trim().isEmpty()) {
            fileReaderTool.setProjectRootPath(projectPath);
        }

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

    /**
     * 代码修改接口
     * 接收代码修改请求，返回修改后的完整代码
     *
     * @param request 包含原始代码和修改指令的请求体
     * @return 修改后的代码
     */
    @PostMapping("/modify-code")
    public Map<String, String> modifyCode(@RequestBody Map<String, String> request) {
        try {
            String originalCode = request.get("originalCode");
            String modificationInstruction = request.get("instruction");
            String fileName = request.getOrDefault("fileName", "");

            if (originalCode == null || originalCode.trim().isEmpty()) {
                return Map.of("error", "原始代码不能为空");
            }

            if (modificationInstruction == null || modificationInstruction.trim().isEmpty()) {
                return Map.of("error", "修改指令不能为空");
            }

            // 构建给AI的完整提示词
            String prompt = buildModificationPrompt(originalCode, modificationInstruction, fileName);

            // 调用AI服务生成修改后的代码
            String modifiedCode = aiCodeHelperService.modifyCode(prompt);

            return Map.of(
                    "modifiedCode", modifiedCode,
                    "status", "success");
        } catch (Exception e) {
            return Map.of(
                    "error", "代码修改失败: " + e.getMessage(),
                    "status", "error");
        }
    }

    /**
     * 代码修改接口（带差异比较）
     * 接收代码修改请求，返回修改后的代码和差异信息
     *
     * @param request 包含原始代码和修改指令的请求体
     * @return 包含差异信息的修改结果
     */
    @PostMapping("/modify-code-with-diff")
    public CodeDiffResult modifyCodeWithDiff(@RequestBody Map<String, String> request) {
        try {
            String originalCode = request.get("originalCode");
            String modificationInstruction = request.get("instruction");
            String fileName = request.getOrDefault("fileName", "");

            if (originalCode == null || originalCode.trim().isEmpty()) {
                CodeDiffResult errorResult = new CodeDiffResult();
                errorResult.setError("原始代码不能为空");
                return errorResult;
            }

            if (modificationInstruction == null || modificationInstruction.trim().isEmpty()) {
                CodeDiffResult errorResult = new CodeDiffResult();
                errorResult.setError("修改指令不能为空");
                return errorResult;
            }

            // 构建给AI的完整提示词
            String prompt = buildModificationPrompt(originalCode, modificationInstruction, fileName);

            // 调用AI服务生成修改后的代码
            String modifiedCode = aiCodeHelperService.modifyCode(prompt);

            // 清理AI返回的代码
            String cleanedModifiedCode = DiffUtils.cleanCode(modifiedCode);

            // 计算差异
            CodeDiffResult diffResult = DiffUtils.compareCode(
                    originalCode, cleanedModifiedCode, modificationInstruction, fileName);

            return diffResult;
        } catch (Exception e) {
            CodeDiffResult errorResult = new CodeDiffResult();
            errorResult.setError("代码修改失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 构建代码修改提示词
     */
    private String buildModificationPrompt(String originalCode, String instruction, String fileName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下指令修改代码。\n\n");

        if (fileName != null && !fileName.isEmpty()) {
            prompt.append("文件名: ").append(fileName).append("\n\n");
        }

        prompt.append("原始代码:\n```\n").append(originalCode).append("\n```\n\n");
        prompt.append("修改指令: ").append(instruction).append("\n\n");
        prompt.append("请直接返回修改后的完整代码，不要添加任何解释。");
        prompt.append("代码必须可以直接使用，保持原有的格式和缩进。");
        prompt.append("如果代码用```java包裹，请去掉包裹标记，只返回纯代码。");

        return prompt.toString();
    }
}