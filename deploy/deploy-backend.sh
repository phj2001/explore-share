#!/bin/bash
# 后端部署脚本
# 使用方式: bash deploy-backend.sh <上传到服务器的jar文件路径>
# 例: bash deploy-backend.sh /tmp/deploy-incoming/smart-campus-backend-0.0.1-SNAPSHOT.jar
set -euo pipefail

RELEASE_DIR="/opt/place-explore/backend/releases"
APP_JAR_LINK="/opt/place-explore/backend/app.jar"
SERVICE_NAME="place-explore-backend.service"
KEEP_RELEASES=5

if [ $# -lt 1 ]; then
  echo "用法: $0 <新jar文件路径>"
  exit 1
fi

INCOMING_JAR="$1"
if [ ! -f "$INCOMING_JAR" ]; then
  echo "错误: 找不到文件 $INCOMING_JAR"
  exit 1
fi

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BASE_NAME=$(basename "$INCOMING_JAR" .jar)
NEW_JAR="${RELEASE_DIR}/${BASE_NAME}-${TIMESTAMP}.jar"

echo "==> 拷贝新版本到 releases 目录"
cp "$INCOMING_JAR" "$NEW_JAR"

echo "==> 切换 app.jar 软链"
ln -sfn "$NEW_JAR" "$APP_JAR_LINK"

echo "==> 重启后端服务"
sudo systemctl restart "$SERVICE_NAME"

echo "==> 等待服务启动..."
sleep 3
if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
  echo "==> 服务启动成功: $(readlink -f $APP_JAR_LINK)"
else
  echo "==> 警告: 服务未处于 active 状态，请检查日志:"
  echo "    sudo journalctl -u $SERVICE_NAME -n 50 --no-pager"
  exit 1
fi

echo "==> 清理旧版本，只保留最近 ${KEEP_RELEASES} 个"
cd "$RELEASE_DIR"
ls -t | tail -n +$((KEEP_RELEASES + 1)) | xargs -r rm -f

echo "==> 完成。当前 releases 目录:"
ls -la "$RELEASE_DIR"
