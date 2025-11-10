#!/bin/bash

# AI Code Helper 服务管理脚本

APP_NAME="ai-code-helper"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示服务状态
show_status() {
    print_info "查询服务状态..."
    systemctl status ${APP_NAME}
}

# 启动服务
start_service() {
    print_info "启动服务..."
    systemctl start ${APP_NAME}
    sleep 2
    if systemctl is-active --quiet ${APP_NAME}; then
        print_info "服务启动成功！"
        show_status
    else
        print_error "服务启动失败！"
        exit 1
    fi
}

# 停止服务
stop_service() {
    print_info "停止服务..."
    systemctl stop ${APP_NAME}
    sleep 2
    if systemctl is-active --quiet ${APP_NAME}; then
        print_error "服务停止失败！"
        exit 1
    else
        print_info "服务已停止"
    fi
}

# 重启服务
restart_service() {
    print_info "重启服务..."
    systemctl restart ${APP_NAME}
    sleep 2
    if systemctl is-active --quiet ${APP_NAME}; then
        print_info "服务重启成功！"
        show_status
    else
        print_error "服务重启失败！"
        exit 1
    fi
}

# 查看日志
show_logs() {
    print_info "显示实时日志 (Ctrl+C 退出)..."
    journalctl -u ${APP_NAME} -f
}

# 查看最近日志
show_recent_logs() {
    print_info "显示最近100行日志..."
    journalctl -u ${APP_NAME} -n 100 --no-pager
}

# 测试连接
test_connection() {
    print_info "测试服务连接..."
    if curl -s http://localhost:8081 > /dev/null; then
        print_info "服务运行正常，端口8081可访问"
    else
        print_warn "无法连接到服务端口8081"
        print_warn "请检查服务是否正在运行: sudo systemctl status ${APP_NAME}"
    fi
}

# 显示帮助信息
show_help() {
    echo "======================================"
    echo "  AI Code Helper 服务管理脚本"
    echo "======================================"
    echo ""
    echo "使用方法: sudo ./manage.sh [命令]"
    echo ""
    echo "可用命令:"
    echo "  start      - 启动服务"
    echo "  stop       - 停止服务"
    echo "  restart    - 重启服务"
    echo "  status     - 查看服务状态"
    echo "  logs       - 查看实时日志"
    echo "  recent     - 查看最近日志"
    echo "  test       - 测试服务连接"
    echo "  help       - 显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  sudo ./manage.sh start"
    echo "  sudo ./manage.sh status"
    echo "  sudo ./manage.sh logs"
    echo ""
}

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then 
        print_error "请使用root权限运行此脚本"
        echo "使用方法: sudo ./manage.sh $1"
        exit 1
    fi
}

# 主逻辑
case "$1" in
    start)
        check_root "start"
        start_service
        ;;
    stop)
        check_root "stop"
        stop_service
        ;;
    restart)
        check_root "restart"
        restart_service
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    recent)
        show_recent_logs
        ;;
    test)
        test_connection
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        show_help
        exit 1
        ;;
esac

exit 0

