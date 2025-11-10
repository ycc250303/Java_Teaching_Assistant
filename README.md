# Java_Teaching_Assistant

2025同济大学Java企业级应用开发期中作业

## 项目结构说明

本项目包含两个主要部分：

### 📦 项目组成

1. **java-teaching-assistant** - IntelliJ IDEA 插件项目

   - 使用 Gradle 构建
   - 提供聊天界面、代码上下文管理、智能代码修改、请求队列系统等功能
   - 共 27 个核心 Java 文件（新增：RequestQueueManager、QueueDisplayPanel、FileDropHandler等）
2. **ai-code-helper** - Spring Boot AI 后端服务

   - 使用 Maven 构建
   - 基于 Langchain4j 框架
   - 提供 AI 对话、RAG 检索、代码修改、自主代码读取、意图识别等功能
   - 共 13 个核心 Java 文件

### 📚 详细文档

**完整的项目结构说明（精确到每个文件）请查看：**
👉 **[项目结构说明.md](项目结构说明.md)**

该文档包含：

- 所有目录和文件的详细列表（最新更新：27个前端核心文件）
- 每个文件的职责和关键功能
- 项目架构关系图
- 核心功能与文件映射表
- 技术栈总结

**功能完成情况清单：**
👉 **[功能完成情况清单.md](功能完成情况清单.md)**

包含：

- F0基础功能完成情况（100%）
- F2扩展功能完成情况（100%）
- 新增功能：请求队列系统、多文件修改等
- 技术亮点和附加功能
- 待开发功能规划（包括多会话管理设计方案）

### 🎉 核心功能状态

- ✅ F0基础功能：100%（RAG、来源引用、UI、上下文、请求队列）
- ✅ F2扩展功能：100%（代码修改、AI意图识别、多文件修改）

---

## 💻 本地开发部署

### 前置准备

1. **安装 Java 21+**

   - 下载并安装 JDK 21 或更高版本
   - 配置 `JAVA_HOME` 环境变量
2. **安装 IntelliJ IDEA**

   - 下载 IDEA 社区版或企业版
   - 安装 Plugin DevKit 插件
3. **获取 API Key**

   - 访问 [通义千问控制台](https://dashscope.console.aliyun.com/apiKey)
   - 注册/登录阿里云账号并开通 DashScope 服务
   - 创建 API Key（格式：`sk-xxxxxxxxxxxxxx`）

### 步骤1: 配置 API Key（必需）

**Windows:**

1. 右键 `此电脑` → `属性` → `高级系统设置` → `环境变量`
2. 在 `用户变量` 中点击 `新建`
3. 变量名：`DASHSCOPE_API_KEY`
4. 变量值：`sk-your-real-api-key-here`
5. 点击 `确定` 保存
6. **重启 IDEA** 使环境变量生效

### 步骤2: 启动后端服务

1. 在 IDEA 中打开 `ai-code-helper` 项目
2. 找到 `src/main/java/com/example/aicodehelper/AiCodeHelperApplication.java`
3. 右键 → `Run 'AiCodeHelperApplication'`
4. 等待服务启动，看到以下日志表示成功：
   ```
   Started AiCodeHelperApplication in X.XXX seconds
   Tomcat started on port 8081
   ```

**或使用命令行启动：**

```batch
cd ai-code-helper
mvnw.cmd spring-boot:run
```

### 步骤3: 运行插件

1. 在 IDEA 中打开 `java-teaching-assistant` 项目
2. 确保后端服务已启动（localhost:8081）
3. 打开 Gradle 面板，找到 `Tasks` → `intellij` → `runIde`
4. 双击运行，会启动一个新的 IDEA 实例（带插件）
5. 在新的 IDEA 实例中：
   - 打开或创建一个 Java 项目
   - 在右侧工具栏找到 `Java AI Assistant` 窗口
   - 开始使用聊天功能

### 验证部署

**测试后端服务：**

```batch
curl http://localhost:8081
```

**测试插件功能：**

1. 在插件窗口中输入 "你好"
2. 应该收到 AI 的回复
3. 尝试添加代码上下文、代码修改等功能

### 修改后端服务地址（可选）

如果需要连接远程服务器而非本地：

1. 打开 `java-teaching-assistant/src/main/java/com/javaProgram/services/AiServiceClient.java`
2. 找到 `USE_REMOTE_SERVER` 常量
3. 修改为 `true` 并设置 `REMOTE_SERVER_URL`：
   ```java
   private static final boolean USE_REMOTE_SERVER = true;
   private static final String REMOTE_SERVER_URL = "http://your-server-ip:8081";
   ```

### 常见问题

**Q: 启动失败，提示 `Could not resolve placeholder 'DASHSCOPE_API_KEY'`**

A: API Key 未正确配置，请按照步骤1重新配置环境变量，并重启 IDEA。

**Q: 插件无法连接后端服务**

A: 检查后端服务是否正常运行（`http://localhost:8081`），确保端口8081未被占用。

**Q: RAG 功能不工作**

A: 确保 `ai-code-helper/src/main/resources/docs/` 目录下有 PDF 文档。首次启动会自动向量化文档。

---

## 🚀 后端部署到Linux服务器

完整的部署指南请查看：

📖 **[部署指南](ai-code-helper/deploy/README.md)** - 完整的部署参考手册（含API Key安全配置）

### 环境要求

**服务器:**

- Linux (Ubuntu 20.04+, CentOS 7+, Debian 10+)
- Java 21+
- 至少 2GB RAM

**本地:**

- Windows 10/11
- Java 21+
- 文件传输工具 (WinSCP/FileZilla)

---

## 插件开发说明

### 环境配置

* 下载 IDEA 社区版/企业版
* 下载 Plugin DevKit 插件
* 新建项目，选择类型 `IDE 插件`
* 点击 `Gradle` 图标构建Gradle，完成后会生成 `.gradle`,`.intelljPlatfrom`,`build`三个文件夹
* 将 `src/main/kotlin`文件夹改为 `src/main/java`

### 重要说明

* `build.gradle.kts`：配置文件，我的IDEA创建的默认设置可以直接跑
* `src/main/resources/MAIN-INF/plugin.xml`：插件信息，包括了插件的id `<id>`、名称 `<name>`、开发者信息 `<vendor>`、描述 `<description>`
* 在 `src/main/java/*`文件夹内创建操作类action后，需要在 `plugin.xml`文件中添加信息，举例：

```xml
    <actions>
        <action id="SelectAction" class="com.example.demo.SelectAction" text="SelectAction"
                description="SelectAction">
            <add-to-group group-id="ToolsMenu" anchor="first"></add-to-group>
        </action>
    </actions>
```

* 说明
  * id：操作的id
  * class：操作的类，要和目录结构一致
  * text：操作名称
  * description：操作描述
  * add-to-group：操作添加的位置，此处的含义是添加到在“工具”菜单项的最上层（在IDEA页面，按下ctrl+alt+鼠标左键可以查看具体UI位置的信息）（不过这部分建议直接问AI）

### 打包插件

* 点击 `Gradle` 图标，打开 `build` 文件夹，点击 `jar`，开始打包插件
* 打包完成后，会输出 `执行完成 'jar'`，此时在 `build\libs` 文件夹下可以找到打包的插件

![1761452351944](image/README/1761452351944.png)

### 安装插件

* 打开插件仓库，点击设置按钮，选择“从磁盘安装插件”，选择对应的jar包进行安装

![1761452322684](image/README/1761452322684.png)

### 发布插件

* [进入jetbrains插件开发平台](https://plugins.jetbrains.com/developers/intellij-platform)，注册账号
* 点击Upload Plugin上传插件，上传插件jar包，编写相关信息
* 上传成功，等待审核
