# Java_Teaching_Assistant
2025同济大学Java企业级应用开发期中作业

## 项目结构说明

### demo - IDEA 插件项目

该目录是一个 IntelliJ IDEA 插件项目，用于演示如何开发 IDEA 插件。

重要子文件夹及作用：

- `src/main/java` - 存放插件的 Java 源代码
  - `com/example/demo` - 包含插件的主要功能实现类
    - `SelectAction.java` - 插件的操作实现类，定义了插件的具体行为
    - `SelectListener.java` - 事件监听器，处理插件中的事件响应

- `src/main/resources` - 存放插件的资源配置文件
  - `META-INF` - 插件的元数据目录
    - `plugin.xml` - 插件的配置文件，定义插件的基本信息、操作、扩展点等

### ai-code-helper - AI 编程助手项目

该目录是一个基于 Spring Boot 和 Langchain4j 的 AI 编程助手项目。

重要子文件夹及作用：

- `src/main/java` - 存放项目 Java 源代码
  - `com/example/aicodehelper` - 项目的根包
    - `AiCodeHelperApplication.java` - Spring Boot 启动类
    - `ai` - AI 相关功能模块
      - `AiCodeHelper.java` - AI 助手核心类
      - `AiCodeHelperService.java` - AI 服务接口
      - `AiCodeHelperServiceFactory.java` - AI 服务工厂类
      - `guardrail` - AI 输入安全控制模块
      - `listener` - AI 模型监听器配置
      - `mcp` - 模型配置包
      - `model` - AI 模型配置
      - `rag` - 检索增强生成(RAG)相关配置
      - `tools` - AI 工具类
    - `config` - 项目配置类
      - 包含 AI 模型配置、跨域配置等
    - `controller` - 控制器层
      - 处理 HTTP 请求，提供 RESTful API 接口

- `src/main/resources` - 存放项目资源文件
  - `docs` - 文档资料目录，包含丰富的编程学习资料
    - `Java 编程学习路线.md` - Java 学习路线指南
    - `程序员常见面试题.md` - 常见面试题集合
    - `鱼皮的求职指南.md` - 求职指导文档
    - `鱼皮的项目学习建议.md` - 项目学习建议文档
  - `system-prompt.txt` - 系统提示词，定义 AI 助手的角色和行为准则
  - `appliction.yml` 模型配置文件，注意api-key要换成自己的

- `src/test/java` - 存放测试代码
  - `com/example/aicodehelper/ai` - AI 相关功能的测试类

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
