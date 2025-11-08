# Java_Teaching_Assistant
2025同济大学Java企业级应用开发期中作业

## 项目结构说明

本项目包含两个主要部分：

### 📦 项目组成

1. **java-teaching-assistant** - IntelliJ IDEA 插件项目
   - 使用 Gradle 构建
   - 提供聊天界面、代码上下文管理、智能代码修改等功能
   - 共 21 个核心 Java 文件

2. **ai-code-helper** - Spring Boot AI 后端服务
   - 使用 Maven 构建
   - 基于 Langchain4j 框架
   - 提供 AI 对话、RAG 检索、代码修改、自主代码读取等功能
   - 共 13 个核心 Java 文件

### 📚 详细文档

**完整的项目结构说明（精确到每个文件）请查看：**
👉 **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)**

该文档包含：
- 所有目录和文件的详细列表
- 每个文件的职责和关键功能
- 项目架构关系图
- 核心功能与文件映射表
- 技术栈总结

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
