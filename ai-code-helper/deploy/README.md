# AI Code Helper 后端部署指南

本指南详细说明如何将 AI Code Helper 后端服务部署到 Linux 服务器，并配置为开机自启动的后台服务。

## 📋 目录

1. [环境要求](#环境要求)
2. [获取 API Key](#获取-api-key)
3. [本地开发配置](#本地开发配置)
4. [本地打包](#本地打包)
5. [上传到服务器](#上传到服务器)
6. [服务器配置](#服务器配置)
7. [服务管理](#服务管理)
8. [API Key 管理](#api-key-管理)
9. [常见问题](#常见问题)
10. [安全最佳实践](#安全最佳实践)

---

## 🔧 环境要求

### 服务器要求

- **操作系统**: Linux (Ubuntu 20.04+, CentOS 7+, Debian 10+ 等)
- **Java**: OpenJDK 21 或更高版本
- **内存**: 至少 2GB RAM (推荐 4GB+)
- **磁盘空间**: 至少 1GB 可用空间
- **网络**: 需要能够访问外部 API (通义千问等)

### 本地环境要求 (Windows)

- **Java**: JDK 21 或更高版本
- **Maven**: 已集成在项目中 (使用 mvnw.cmd)
- **文件传输工具**: WinSCP、FileZilla 或 scp 命令，

---

## 🔑 获取 API Key

在部署前，你需要先获取通义千问的 API Key。

### 通义千问（阿里云 DashScope）

1. 访问：https://dashscope.console.aliyun.com/apiKey
2. 注册/登录阿里云账号
3. 开通 DashScope 服务
4. 创建 API Key
5. 复制 API Key（格式：`sk-xxxxxxxxxxxxxx`）

⚠️ **重要提示**：

- API Key 是敏感信息，请妥善保管
- 不要将 API Key 提交到 Git 仓库
- 不要在公共场合（截图、日志）泄露

---

## 💻 本地开发配置

在本地开发和测试时，需要配置 API Key 环境变量。

### 方式1：IDEA 运行配置（推荐）

在 IntelliJ IDEA 中配置环境变量（无需修改系统环境变量）：

1. 打开 `Run/Debug Configurations`
2. 选择 Spring Boot 运行配置（`AiCodeHelperApplication`）
3. 点击 `Modify options` → 勾选 `Environment variables`
4. 在 `Environment variables` 字段中添加：
   ```
   DASHSCOPE_API_KEY=sk-your-real-api-key-here
   ```
5. 点击 `Apply` → `OK`
6. 启动应用

### 方式2：系统环境变量（永久配置）

**Windows 图形界面配置：**

1. 右键 `此电脑` → `属性` → `高级系统设置` → `环境变量`
2. 在 `用户变量` 中点击 `新建`：
   - 变量名：`DASHSCOPE_API_KEY`
   - 变量值：`sk-your-real-api-key-here`
3. 点击 `确定` 保存
4. **重启 IDEA** 使环境变量生效

### 方式3：命令行临时配置

**CMD (Windows):**

```cmd
set DASHSCOPE_API_KEY=sk-your-real-api-key-here
cd ai-code-helper
mvnw.cmd spring-boot:run
```

**PowerShell:**

```powershell
$env:DASHSCOPE_API_KEY="sk-your-real-api-key-here"
cd ai-code-helper
.\mvnw.cmd spring-boot:run
```

### 验证配置

**方法1：命令行验证**

```cmd
# CMD
echo %DASHSCOPE_API_KEY%

# PowerShell
echo $env:DASHSCOPE_API_KEY
```

**方法2：启动应用验证**

在 IDEA 中启动 Spring Boot 应用，查看控制台日志：

- ✅ 成功：`Started AiCodeHelperApplication in X.XXX seconds`
- ❌ 失败：`Could not resolve placeholder 'DASHSCOPE_API_KEY'`

---

## 📦 本地打包

### 执行打包脚本

在 Windows 本地，打开命令提示符 (CMD)，进入项目目录：

```batch
cd Java_Teaching_Assistant\ai-code-helper\deploy
deploy.bat
```

**脚本会自动执行以下操作：**

1. 清理旧的构建文件
2. 使用 Maven 打包项目 (跳过测试)
3. 检查 JAR 文件是否生成成功
4. 显示上传提示信息

**打包完成后，JAR 文件位置：**

```
ai-code-helper\target\ai-code-helper-0.0.1-SNAPSHOT.jar
```

---

## 📤 上传到服务器

### 使用 FileZilla或其他工具

1. **下载并安装 FileZilla**: https://filezilla-project.org/
2. 使用 SFTP 协议连接服务器
3. 拖拽上传文件

---

## ⚙️ 服务器配置

### 步骤1: 连接到服务器

使用 SSH 客户端连接到服务器 (推荐使用 PuTTY 或 Windows Terminal)：

```bash
ssh root@YOUR_SERVER_IP
```

### 步骤2: 安装 Java (如果未安装)

**Ubuntu/Debian:**

```bash
sudo apt update
sudo apt install openjdk-21-jdk -y
java -version
```

**CentOS/RHEL:**

```bash
sudo yum install java-21-openjdk -y
java -version
```

### 步骤3: 创建应用目录

```bash
mkdir -p /opt/ai-code-helper
cd /opt/ai-code-helper
```

### 步骤4: 赋予脚本执行权限

```bash
chmod +x install-service.sh
chmod +x manage.sh
```

### 步骤5: 运行安装脚本

```bash
sudo ./install-service.sh
```

**安装脚本会自动执行以下操作：**

1. ✅ 检查 Java 环境
2. ✅ 创建应用目录 (`/opt/ai-code-helper`)
3. ✅ 创建日志目录 (`/var/log/ai-code-helper`)
4. ✅ 检查 JAR 文件
5. ✅ 安装 systemd 服务
6. ✅ 设置开机自启动

### 步骤6: 配置 API Key（重要）

⚠️ **这是必须步骤，否则服务无法正常启动！**

编辑 systemd 服务配置文件：

```bash
sudo nano /etc/systemd/system/ai-code-helper.service
```

找到 `[Service]` 部分，修改 `Environment` 行，填入你的真实 API Key：

```ini
[Service]
Type=simple
User=root
WorkingDirectory=/opt/ai-code-helper
ExecStart=/usr/bin/java -jar -Xms512m -Xmx2048m -Dspring.profiles.active=prod /opt/ai-code-helper/ai-code-helper.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=ai-code-helper

# ========== 修改这里的 API Key（必需）==========
Environment="DASHSCOPE_API_KEY=sk-your-real-dashscope-api-key-here"
# ==============================================
```

保存文件：

- 按 `Ctrl+O` 保存
- 按 `Enter` 确认
- 按 `Ctrl+X` 退出

重新加载 systemd 配置：

```bash
sudo systemctl daemon-reload
```

### 步骤7: 创建并上传 RAG 文档（可选）

如果需要使用 RAG（检索增强生成）功能，需要创建文档目录并上传 PDF 文件：

```bash
# 创建文档目录
mkdir -p /opt/ai-code-helper/docs

# 上传 PDF 文件到 /opt/ai-code-helper/docs/
# 可以使用 WinSCP、FileZilla 或 scp 命令
```

如果跳过此步骤，RAG 功能将被禁用，但不影响其他功能正常使用。

### 步骤8: 启动服务

```bash
sudo systemctl start ai-code-helper
```

### 步骤9: 检查服务状态

```bash
sudo systemctl status ai-code-helper
```

**正常运行的输出示例：**

```
● ai-code-helper.service - Java Teaching Assistant - AI Code Helper Backend Service
   Loaded: loaded (/etc/systemd/system/ai-code-helper.service; enabled)
   Active: active (running) since Mon 2025-01-01 10:00:00 UTC; 5s ago
 Main PID: 12345 (java)
   CGroup: /system.slice/ai-code-helper.service
           └─12345 /usr/bin/java -jar ...
```

**验证 API Key 是否正确加载：**

```bash
# 检查环境变量
sudo systemctl show ai-code-helper | grep Environment

# 查看启动日志（确认没有 API Key 相关错误）
sudo journalctl -u ai-code-helper -n 50
```

### 步骤10: 配置防火墙 (如果需要外部访问)

**Ubuntu/Debian (使用 ufw):**

```bash
sudo ufw allow 8081/tcp
sudo ufw reload
```

**CentOS/RHEL (使用 firewalld):**

```bash
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

**云服务器安全组配置：**

如果使用阿里云、腾讯云等云服务器，还需要在控制台配置安全组规则：
- 协议：TCP
- 端口：8081
- 来源：0.0.0.0/0（允许所有 IP 访问）

### 步骤11: 测试服务

```bash
curl http://localhost:8081
```

或者从外部访问 (替换为你的服务器 IP):

```bash
curl http://YOUR_SERVER_IP:8081
```

---

## 🛠️ 服务管理

### 使用管理脚本 (推荐)

我们提供了一个便捷的管理脚本 `manage.sh`：

```bash
# 启动服务
sudo ./manage.sh start

# 停止服务
sudo ./manage.sh stop

# 重启服务
sudo ./manage.sh restart

# 查看状态
sudo ./manage.sh status

# 查看实时日志 (Ctrl+C 退出)
sudo ./manage.sh logs

# 查看最近日志
sudo ./manage.sh recent

# 测试服务连接
sudo ./manage.sh test

# 显示帮助信息
sudo ./manage.sh help
```

### 使用 systemd 命令

```bash
# 启动服务
sudo systemctl start ai-code-helper

# 停止服务
sudo systemctl stop ai-code-helper

# 重启服务
sudo systemctl restart ai-code-helper

# 查看状态
sudo systemctl status ai-code-helper

# 查看日志
sudo journalctl -u ai-code-helper -f

# 启用开机自启
sudo systemctl enable ai-code-helper

# 禁用开机自启
sudo systemctl disable ai-code-helper
```

### 查看日志

**方式1: systemd 日志**

```bash
# 实时查看日志
sudo journalctl -u ai-code-helper -f

# 查看最近100行日志
sudo journalctl -u ai-code-helper -n 100

# 查看今天的日志
sudo journalctl -u ai-code-helper --since today
```

**方式2: 应用日志文件**

```bash
# 实时查看应用日志
sudo tail -f /var/log/ai-code-helper/application.log

# 查看最近100行
sudo tail -n 100 /var/log/ai-code-helper/application.log
```

---

## 🔄 更新部署

当你更新了代码需要重新部署时：

### 1. 本地重新打包

```batch
cd D:\GitHub\ycc\Java_Teaching_Assistant\ai-code-helper\deploy
deploy.bat
```

### 2. 上传新的 JAR 文件到服务器

使用 WinSCP、scp 或 FileZilla 上传

### 3. 重启服务

```bash
ssh root@YOUR_SERVER_IP
cd /opt/ai-code-helper
sudo ./manage.sh restart
```

---

## 🔑 API Key 管理

### 更新 API Key

**本地开发环境：**

**更新系统环境变量：**

1. 右键 `此电脑` → `属性` → `高级系统设置` → `环境变量`
2. 在 `用户变量` 中找到 `DASHSCOPE_API_KEY`
3. 双击编辑，修改变量值
4. 点击 `确定` 保存
5. **重启 IDEA** 使新的环境变量生效

**服务器生产环境：**

```bash
# 1. 编辑 systemd 服务配置
sudo nano /etc/systemd/system/ai-code-helper.service

# 2. 找到并修改 Environment 行中的 API Key
Environment="DASHSCOPE_API_KEY=sk-your-new-api-key-here"

# 3. 保存文件（Ctrl+O, Enter, Ctrl+X）

# 4. 重新加载并重启服务
sudo systemctl daemon-reload
sudo systemctl restart ai-code-helper

# 5. 验证服务状态
sudo ./manage.sh status
```

### API Key 泄露应对

如果不小心泄露了 API Key：

1. **立即登录 API 提供商控制台**
   - 通义千问：https://dashscope.console.aliyun.com/apiKey
2. **删除或重置泄露的 API Key**
3. **创建新的 API Key**
4. **按照上述步骤更新本地和服务器的 API Key**
5. **检查是否有异常调用记录**

---

## ❓ 常见问题

### 1. 服务启动失败 - API Key 未配置

**错误日志：**

```
Could not resolve placeholder 'DASHSCOPE_API_KEY'
```

**解决方法：**

检查 API Key 是否正确配置：

```bash
# 查看环境变量
sudo systemctl show ai-code-helper | grep Environment

# 编辑服务配置
sudo nano /etc/systemd/system/ai-code-helper.service

# 添加或修改 Environment 行
Environment="DASHSCOPE_API_KEY=sk-your-real-api-key-here"

# 重新加载并重启
sudo systemctl daemon-reload
sudo systemctl restart ai-code-helper
```

### 2. RAG 文档目录不存在

**错误日志：**

```
文档目录不存在: /opt/ai-code-helper/docs
未找到任何文档，RAG功能将无法正常工作
```

**解决方法：**

```bash
# 创建文档目录
mkdir -p /opt/ai-code-helper/docs

# 上传 PDF 文件到该目录（使用 WinSCP、FileZilla 或 scp）

# 重启服务
sudo systemctl restart ai-code-helper
```

### 3. 服务启动失败 - 其他原因

**检查 Java 版本:**

```bash
java -version
```

确保是 Java 21 或更高版本

**检查 JAR 文件:**

```bash
ls -lh /opt/ai-code-helper/ai-code-helper.jar
```

**查看详细错误日志:**

```bash
sudo journalctl -u ai-code-helper -n 50
```

### 4. 端口被占用

**检查端口占用:**

```bash
sudo netstat -tulpn | grep 8081
```

**修改端口** (编辑 `application-prod.yml`):

```yaml
server:
  port: 8082  # 改为其他端口
```

### 5. 内存不足

**修改 JVM 内存配置** (编辑 `ai-code-helper.service`):

```ini
ExecStart=/usr/bin/java -jar -Xms256m -Xmx1024m -Dspring.profiles.active=prod /opt/ai-code-helper/ai-code-helper.jar
```

重新加载配置:

```bash
sudo systemctl daemon-reload
sudo systemctl restart ai-code-helper
```

### 6. 无法访问外部 API

**检查网络连接:**

```bash
curl -I https://dashscope.aliyuncs.com
```

**检查 DNS:**

```bash
ping dashscope.aliyuncs.com
```

### 7. 日志文件权限问题

**创建日志目录并设置权限:**

```bash
sudo mkdir -p /var/log/ai-code-helper
sudo chmod 755 /var/log/ai-code-helper
```

### 8. 服务无法自动重启

**检查 systemd 配置:**

```bash
sudo systemctl cat ai-code-helper
```

确保包含:

```ini
Restart=on-failure
RestartSec=10
```

---

## 🔒 安全最佳实践

### API Key 安全

**✅ 推荐做法：**

1. **使用环境变量** - 永远不要在代码或配置文件中硬编码 API Key
2. **使用 .gitignore** - 确保敏感文件不会被提交到 Git
   ```gitignore
   # 向量数据缓存
   embedding-store.json
   document-fingerprint.json
   
   # 包含真实密钥的配置文件
   application-local.yml
   *-secret.yml
   ```
3. **最小权限原则** - 服务器上的配置文件设置为 `600` 权限
   ```bash
   sudo chmod 600 /etc/systemd/system/ai-code-helper.service
   ```
4. **定期轮换** - 定期更换 API Key
5. **监控使用** - 在 API 提供商控制台监控异常使用

**❌ 禁止做法：**

1. ❌ 将 API Key 硬编码在代码中
2. ❌ 将 API Key 提交到 Git 仓库
3. ❌ 在公共场合（截图、日志）泄露 API Key
4. ❌ 使用弱权限保存密钥文件
5. ❌ 在多个项目共用同一个 API Key

### 服务器安全

**1. 限制外部访问**

如果不需要外部访问，使用防火墙限制只允许本地访问:

```bash
# Ubuntu/Debian
sudo ufw deny 8081

# CentOS/RHEL
sudo firewall-cmd --permanent --remove-port=8081/tcp
sudo firewall-cmd --reload
```

**2. 使用非 root 用户运行（推荐）**

创建专用用户:

```bash
sudo useradd -r -s /bin/false ai-helper
sudo chown -R ai-helper:ai-helper /opt/ai-code-helper
sudo chown -R ai-helper:ai-helper /var/log/ai-code-helper
```

修改服务配置:

```bash
sudo nano /etc/systemd/system/ai-code-helper.service
```

修改 User 字段:

```ini
[Service]
User=ai-helper
```

重新加载并重启:

```bash
sudo systemctl daemon-reload
sudo systemctl restart ai-code-helper
```

**3. 定期更新**

定期更新系统和 Java 运行环境:

```bash
# Ubuntu/Debian
sudo apt update && sudo apt upgrade -y

# CentOS/RHEL
sudo yum update -y
```

**4. 启用日志审计**

定期检查应用日志，发现异常行为:

```bash
# 查看最近的访问日志
sudo journalctl -u ai-code-helper --since "1 hour ago"

# 查找错误日志
sudo journalctl -u ai-code-helper -p err
```

---

## 📝 文件清单

部署相关文件列表:

```
ai-code-helper/deploy/
├── README.md                    # 本部署指南（含 API Key 安全配置）
├── deploy.bat                   # Windows 本地打包脚本
├── ai-code-helper.service       # systemd 服务配置文件
├── install-service.sh           # 服务器安装脚本
└── manage.sh                    # 服务管理脚本
```

---

## 🎯 快速部署总结

### 本地开发

1. 获取 API Key：https://dashscope.console.aliyun.com/apiKey
2. 配置环境变量：
   - IDEA：`Run/Debug Configurations` → `Environment variables`
   - 系统：`此电脑` → `属性` → `高级系统设置` → `环境变量`
   - 变量名：`DASHSCOPE_API_KEY`
   - 变量值：`sk-your-api-key`
3. 启动应用测试

### 服务器部署

1. 本地打包：`deploy.bat`
2. 上传文件：JAR + 脚本 → `/opt/ai-code-helper/`
3. 配置 API Key：编辑 `/etc/systemd/system/ai-code-helper.service`
4. 启动服务：`sudo ./install-service.sh && sudo systemctl start ai-code-helper`
5. 配置防火墙：开放 8081 端口
6. 测试访问：`curl http://YOUR_SERVER_IP:8081`

---

**祝部署顺利！🚀**

**保护好你的 API Key 就像保护你的密码一样重要！** 🔐
