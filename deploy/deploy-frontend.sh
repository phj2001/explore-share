#!/bin/bash
# 前端部署脚本
# 使用方式: bash deploy-frontend.sh <上传到服务器的dist目录路径>
# 例: bash deploy-frontend.sh /tmp/deploy-incoming/dist
set -euo pipefail

RELEASES_DIR="/var/www/place-explore/releases"
CURRENT_LINK="/var/www/place-explore/current"
KEEP_RELEASES=5

if [ $# -lt 1 ]; then
  echo "用法: $0 <新dist目录路径>"
  exit 1
fi

INCOMING_DIST="$1"
if [ ! -d "$INCOMING_DIST" ]; then
  echo "错误: 找不到目录 $INCOMING_DIST"
  exit 1
fi

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
NEW_RELEASE="${RELEASES_DIR}/frontend-${TIMESTAMP}"

echo "==> 创建新版本目录: $NEW_RELEASE"
mkdir -p "$NEW_RELEASE"
cp -r "$INCOMING_DIST"/* "$NEW_RELEASE/"

echo "==> 切换 current 软链"
ln -sfn "$NEW_RELEASE" "$CURRENT_LINK"

echo "==> 完成。当前指向: $(readlink -f $CURRENT_LINK)"

echo "==> 清理旧版本，只保留最近 ${KEEP_RELEASES} 个"
cd "$RELEASES_DIR"
ls -dt frontend-*/ 2>/dev/null | tail -n +$((KEEP_RELEASES + 1)) | xargs -r rm -rf
# 顺手清理历史遗留的 zip 包（如果有的话，只留最近的）
ls -t frontend-dist-*.zip 2>/dev/null | tail -n +$((KEEP_RELEASES + 1)) | xargs -r rm -f

echo "==> 完成。当前 releases 目录:"
ls -la "$RELEASES_DIR"
