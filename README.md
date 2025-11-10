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

## 🎯 最新更新 (2025-11-10)

### ✨ 新增功能
1. **请求队列系统** 🚀
   - AI响应时输入框保持可用
   - 支持最多3个请求排队等待
   - 实时可视化队列状态（⚙️处理中、⏳等待中）
   - 自动串行处理保证顺序

2. **文件拖拽和粘贴增强** 📎
   - 支持从文件系统拖拽文件到输入框
   - 支持从Project视图拖拽文件
   - 智能识别粘贴的代码块格式
   - 自动添加到代码上下文

3. **多文件修改优化** 📝
   - 串行处理保证气泡显示顺序
   - 每个文件独立的diff查看和确认

4. **多会话管理设计** 💬
   - 前后端协同方案已完成
   - 支持会话列表、切换、持久化
   - 详见：功能完成情况清单.md

### 🎉 核心功能状态
- ✅ F0基础功能：100%（RAG、来源引用、UI、上下文、请求队列）
- ✅ F2扩展功能：100%（代码修改、AI意图识别、多文件修改）

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
