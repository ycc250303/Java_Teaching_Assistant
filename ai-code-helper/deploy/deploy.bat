@echo off
chcp 65001 >nul

REM ============================================
REM  【重要】部署脚本配置说明
REM ============================================
REM
REM 如果你需要在此脚本中填写真实的服务器信息（IP地址、密码等），
REM 请按以下步骤操作以确保安全：
REM
REM 1. 在下方配置区域中临时修改服务器信息
REM 2. 使用完后立即恢复为占位符
REM 3. 不要提交包含真实密码的文件到Git仓库
REM
REM ⚠️ 安全提示：
REM - 永远不要将包含真实密码的文件提交到Git仓库
REM - 如果不小心提交了，请立即修改服务器密码
REM - 建议使用SSH密钥认证代替密码认证
REM
REM 配置项说明：
REM -----------
REM SERVER_IP       - 服务器IP地址（例如：111.229.81.45）
REM SERVER_USER     - SSH用户名（通常是 root）
REM SERVER_PASSWORD - SSH密码（仅用于提示，实际上传需要手动操作）
REM REMOTE_DIR      - 服务器上的部署目录（默认：/opt/ai-code-helper）
REM LOCAL_JAR       - 本地JAR文件路径（默认：target\ai-code-helper-0.0.1-SNAPSHOT.jar）
REM
REM 注意：
REM deploy.bat 脚本本身不会使用密码进行自动上传，
REM 它只会打包JAR文件并提示你如何手动上传。
REM ============================================

echo ====================================
echo   AI Code Helper 部署脚本 (Windows)
echo ====================================
echo.

REM ============================================
REM  配置区域 - 请根据实际情况修改
REM ============================================
set SERVER_IP=your.server.ip.address
set SERVER_USER=root
set SERVER_PASSWORD=your_password
set REMOTE_DIR=/opt/ai-code-helper
set LOCAL_JAR=target\ai-code-helper-0.0.1-SNAPSHOT.jar
REM ============================================

REM 保存当前目录并切换到项目根目录
set DEPLOY_DIR=%CD%
cd ..

echo [1/5] 清理旧的构建文件...
call mvnw.cmd clean
if errorlevel 1 (
    echo 清理失败！
    cd /d "%DEPLOY_DIR%"
    pause
    exit /b 1
)

echo.
echo [2/5] 打包项目 (跳过测试)...
call mvnw.cmd package -DskipTests
if errorlevel 1 (
    echo 打包失败！
    cd /d "%DEPLOY_DIR%"
    pause
    exit /b 1
)

echo.
echo [3/5] 检查JAR文件...
if not exist "%LOCAL_JAR%" (
    echo 错误: 找不到JAR文件 %LOCAL_JAR%
    cd /d "%DEPLOY_DIR%"
    pause
    exit /b 1
)

echo.
echo [4/5] 上传到服务器...
echo 注意: 此步骤需要手动操作！
echo.
echo 请使用以下方式之一上传文件到服务器：
echo.
echo 方式1: 使用WinSCP或FileZilla等FTP工具
echo   - 连接服务器: %SERVER_IP%
echo   - 用户名: %SERVER_USER%
echo   - 上传文件: %LOCAL_JAR%
echo   - 目标目录: %REMOTE_DIR%
echo   - 重命名为: ai-code-helper.jar
echo.
echo 方式2: 使用scp命令 (需要安装OpenSSH客户端)
echo   scp %LOCAL_JAR% %SERVER_USER%@%SERVER_IP%:%REMOTE_DIR%/ai-code-helper.jar
echo.
echo 方式3: 使用pscp命令 (PuTTY工具集)
echo   pscp %LOCAL_JAR% %SERVER_USER%@%SERVER_IP%:%REMOTE_DIR%/ai-code-helper.jar
echo.
echo [5/5] 后续操作提示...
echo.
echo 上传完成后，请在服务器上执行以下命令：
echo   cd /opt/ai-code-helper
echo   chmod +x *.sh
echo   sudo ./install-service.sh
echo   sudo systemctl start ai-code-helper
echo   sudo systemctl status ai-code-helper
echo.
echo ====================================
echo   打包完成！JAR文件位于：
echo   %CD%\%LOCAL_JAR%
echo.
echo   部署脚本位于：
echo   %DEPLOY_DIR%\
echo ====================================

REM 恢复原始目录
cd /d "%DEPLOY_DIR%"
pause

