#!/bin/bash

echo "===================================="
echo "  AI Code Helper 服务安装脚本"
echo "===================================="
echo ""

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    echo "错误: 请使用root权限运行此脚本"
    echo "使用方法: sudo ./install-service.sh"
    exit 1
fi

# 配置变量
APP_NAME="ai-code-helper"
APP_DIR="/opt/${APP_NAME}"
SERVICE_FILE="${APP_NAME}.service"
LOG_DIR="/var/log/${APP_NAME}"

echo "[1/6] 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未检测到Java环境"
    echo "请先安装Java 21或更高版本"
    echo "安装命令 (Ubuntu/Debian): sudo apt install openjdk-21-jdk"
    echo "安装命令 (CentOS/RHEL): sudo yum install java-21-openjdk"
    exit 1
fi

java -version
echo ""

echo "[2/6] 创建应用目录..."
mkdir -p ${APP_DIR}
echo "目录已创建: ${APP_DIR}"
echo ""

echo "[3/6] 创建日志目录..."
mkdir -p ${LOG_DIR}
chmod 755 ${LOG_DIR}
echo "日志目录已创建: ${LOG_DIR}"
echo ""

echo "[4/6] 检查JAR文件..."
if [ ! -f "${APP_DIR}/${APP_NAME}.jar" ]; then
    echo "警告: 未找到JAR文件 ${APP_DIR}/${APP_NAME}.jar"
    echo "请确保已将JAR文件上传到 ${APP_DIR}/ 目录"
    read -p "是否继续安装服务配置? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
echo ""

echo "[5/6] 安装systemd服务..."
if [ ! -f "${SERVICE_FILE}" ]; then
    echo "错误: 未找到服务配置文件 ${SERVICE_FILE}"
    echo "请确保 ${SERVICE_FILE} 文件存在于当前目录"
    exit 1
fi

# 复制服务文件到systemd目录
cp ${SERVICE_FILE} /etc/systemd/system/
chmod 644 /etc/systemd/system/${SERVICE_FILE}

# 重新加载systemd配置
systemctl daemon-reload

echo "服务已安装: ${APP_NAME}"
echo ""

echo "[6/6] 设置服务开机自启..."
systemctl enable ${APP_NAME}
echo ""

echo "===================================="
echo "  安装完成！"
echo "===================================="
echo ""
echo "常用命令:"
echo "  启动服务: sudo systemctl start ${APP_NAME}"
echo "  停止服务: sudo systemctl stop ${APP_NAME}"
echo "  重启服务: sudo systemctl restart ${APP_NAME}"
echo "  查看状态: sudo systemctl status ${APP_NAME}"
echo "  查看日志: sudo journalctl -u ${APP_NAME} -f"
echo "  查看文件日志: tail -f ${LOG_DIR}/application.log"
echo ""
echo "端口信息:"
echo "  默认端口: 8081"
echo "  测试连接: curl http://localhost:8081"
echo ""
echo "防火墙配置 (如果需要外部访问):"
echo "  Ubuntu/Debian: sudo ufw allow 8081"
echo "  CentOS/RHEL: sudo firewall-cmd --permanent --add-port=8081/tcp"
echo "               sudo firewall-cmd --reload"
echo ""

