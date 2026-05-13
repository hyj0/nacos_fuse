#!/bin/bash
# Nacos FUSE 启动脚本 (Linux/Mac) - 支持多服务器配置

echo "========================================"
echo "Nacos FUSE File System - Build and Run"
echo "========================================"
echo ""

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven not found in PATH"
    echo "Please install Maven"
    exit 1
fi

# 编译项目
echo "[INFO] Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "[ERROR] Build failed"
    exit 1
fi

echo ""
echo "[SUCCESS] Build completed"
echo ""

# 设置默认参数
CONFIG_FILE="config.yaml"
MOUNT_POINT="/tmp/nacos-config"

# 如果提供了参数，使用参数
if [ ! -z "$1" ]; then
    CONFIG_FILE="$1"
fi
if [ ! -z "$2" ]; then
    MOUNT_POINT="$2"
fi

# 创建挂载点目录
if [ ! -d "$MOUNT_POINT" ]; then
    echo "[INFO] Creating mount point directory: $MOUNT_POINT"
    mkdir -p "$MOUNT_POINT"
fi

echo "========================================"
echo "Starting Nacos FUSE (Multi-Server)..."
echo "Config File: $CONFIG_FILE"
echo "Mount Point: $MOUNT_POINT"
echo "========================================"
echo ""

# 运行程序
java -jar target/nacos_fuse-1.0-SNAPSHOT.jar "$CONFIG_FILE" "$MOUNT_POINT"
