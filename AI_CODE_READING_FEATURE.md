# AI 自主代码读取功能说明

## 🎯 功能概述

现在AI助手具备了**自主读取项目代码**的能力！不再需要用户手动添加代码到上下文，AI可以根据需要主动读取项目文件。

## ✨ 核心能力

### 1. 列出项目文件结构
AI可以调用 `listProjectFiles` 工具查看项目目录结构。

**示例对话：**
```
用户：项目结构是什么样的？
AI：💻 信息来源：项目代码分析
    
    让我查看一下项目结构...
    
    📂 项目根目录包含：
    📁 src/
    📁 build/
    📄 build.gradle.kts
    ...
```

### 2. 读取具体文件内容
AI可以调用 `readProjectFile` 工具读取任意代码文件。

**示例对话：**
```
用户：ChatToolWindowContent.java 这个类是做什么的？
AI：💻 信息来源：项目代码分析
    
    这个类是聊天工具窗口的主控制器，职责包括：
    1. 协调各个UI组件
    2. 处理用户消息发送
    3. 管理AI响应...
    
    [AI会自动读取文件内容并进行分析]
```

### 3. 搜索代码
AI可以调用 `searchCodeInProject` 工具在项目中搜索特定代码。

**示例对话：**
```
用户：项目里哪些地方使用了 @Tool 注解？
AI：💻 信息来源：项目代码分析
    
    找到以下使用 @Tool 注解的地方：
    1. InterviewQuestionTool.java (第25行)
    2. FileReaderTool.java (第48行、第78行、第115行)
    ...
```

## 🚀 快速开始

### 第1步：启动后端
```bash
cd ai-code-helper
mvn spring-boot:run
```

### 第2步：构建并运行插件
```bash
cd java-teaching-assistant
gradlew.bat runIde
```

### 第3步：测试AI自主读取代码

在测试IDE中打开你的插件，尝试以下对话：

#### 测试1：查看项目结构
```
用户：列出 src/main/java/com/javaProgram 目录下的内容
```

#### 测试2：分析具体文件
```
用户：ChatToolWindowContent.java 的职责是什么？
```

#### 测试3：搜索代码
```
用户：项目里有哪些 Service 类？
```

#### 测试4：修改代码建议
```
用户：MessageBubbleFactory 类应该如何优化？
```

**AI会自动：**
1. 读取 `MessageBubbleFactory.java` 文件
2. 分析代码结构
3. 提供具体的优化建议

## 🛠️ 技术实现

### 架构图

```
┌─────────────────────────────────────────────────┐
│  IntelliJ Plugin (前端)                         │
│  ┌───────────────────────────────┐              │
│  │ ChatToolWindowContent         │              │
│  │  - 获取项目路径               │              │
│  │  - 发送给后端                 │              │
│  └───────────────────────────────┘              │
└─────────────────────┬───────────────────────────┘
                      │ HTTP POST
                      │ (message + projectPath)
                      ▼
┌─────────────────────────────────────────────────┐
│  Spring Boot 后端                               │
│  ┌───────────────────────────────┐              │
│  │ AiController                  │              │
│  │  - 接收项目路径               │              │
│  │  - 设置 FileReaderTool        │              │
│  └────────────┬──────────────────┘              │
│               │                                  │
│               ▼                                  │
│  ┌───────────────────────────────┐              │
│  │ Langchain4j AI Service        │              │
│  │  - Tool: listProjectFiles     │              │
│  │  - Tool: readProjectFile      │              │
│  │  - Tool: searchCodeInProject  │              │
│  └───────────────────────────────┘              │
└─────────────────────────────────────────────────┘
```

### 关键文件

#### 后端
- `FileReaderTool.java` - 代码读取工具（3个工具函数）
- `AiController.java` - 接收项目路径并设置到工具
- `AiCodeHelperServiceFactory.java` - 注册工具到AI服务
- `system-prompt.txt` - 指导AI使用工具

#### 前端
- `ChatToolWindowContent.java` - 获取并发送项目路径
- `AiServiceClient.java` - 携带项目路径的HTTP请求

## 📝 开发说明

### 添加新的代码工具

如果想添加更多代码读取能力，在 `FileReaderTool.java` 中添加新方法：

```java
@Tool(name = "yourToolName", value = """
        Tool description for AI to understand when to use it.
        """
)
public String yourToolMethod(@P(value = "parameter description") String param) {
    // 实现逻辑
    return result;
}
```

### 工具使用最佳实践

1. **清晰的描述**：在 `@Tool` 注解中提供清晰的功能描述
2. **参数说明**：使用 `@P` 注解说明参数含义
3. **格式化输出**：返回易于AI理解的格式化文本
4. **错误处理**：妥善处理文件不存在、权限等异常

## 🎓 与功能2的关系

这个功能是**功能2（智能判断对话/修改代码）的前置需求**：

### 为什么需要AI自主读取代码？

**场景1：用户只说文件名**
```
用户：帮我优化 ChatToolWindowContent
     ❌ 旧方案：需要用户手动添加代码到上下文
     ✅ 新方案：AI自动读取文件，理解后提供建议
```

**场景2：AI需要更多上下文**
```
用户：这个类的 bubbleFactory 是从哪来的？
     ❌ 旧方案：用户需要再次添加相关代码
     ✅ 新方案：AI自动搜索并读取相关定义
```

### 接下来的步骤

有了代码读取能力后，可以实现：

1. **智能意图识别**：AI根据用户问题，判断是要"解释"还是"修改"
2. **自动获取上下文**：AI自动读取相关代码，而不是依赖用户提供
3. **统一入口**：用户只需在聊天框输入需求，AI自动处理

## 🐛 故障排查

### 问题1：AI不使用工具
**可能原因：**
- system-prompt.txt 没有正确加载
- 工具没有正确注册到 AiServices

**解决：**
检查控制台日志，确认 `FileReaderTool initialized with project root: ...`

### 问题2：读取文件失败
**可能原因：**
- 项目路径不正确
- 文件路径拼接错误（Windows vs Unix路径）

**解决：**
在 `FileReaderTool` 中添加日志，查看实际路径

### 问题3：前端没有发送项目路径
**可能原因：**
- `project.getBasePath()` 返回 null

**解决：**
确保在有效的项目上下文中运行插件

## 📚 参考资料

- [Langchain4j Tool Documentation](https://docs.langchain4j.dev/tutorials/tools)
- [IntelliJ Platform Project API](https://plugins.jetbrains.com/docs/intellij/project.html)

## 🎉 总结

现在你的AI助手可以：
- ✅ 自主探索项目结构
- ✅ 主动读取需要的代码文件
- ✅ 在项目中搜索相关代码
- ✅ 基于实际代码提供准确的分析和建议

这为后续实现**智能判断对话/修改代码**功能奠定了坚实基础！🚀

