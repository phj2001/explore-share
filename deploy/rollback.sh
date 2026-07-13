#!/bin/bash
# 回滚脚本
# 用法:
#   查看可回滚版本: bash rollback.sh list
#   回滚后端:       bash rollback.sh backend <jar文件名>
#   回滚前端:       bash rollback.sh frontend <release目录名>
set -euo pipefail

BACKEND_RELEASE_DIR="/opt/place-explore/backend/releases"
BACKEND_LINK="/opt/place-explore/backend/app.jar"
FRONTEND_RELEASES_DIR="/var/www/place-explore/releases"
FRONTEND_LINK="/var/www/place-explore/current"
SERVICE_NAME="place-explore-backend.service"

case "${1:-}" in
  list)
    echo "== 后端可回滚版本 (${BACKEND_RELEASE_DIR}) =="
    ls -t "$BACKEND_RELEASE_DIR"
    echo ""
    echo "== 前端可回滚版本 (${FRONTEND_RELEASES_DIR}) =="
    ls -dt "${FRONTEND_RELEASES_DIR}"/frontend-*/ 2>/dev/null
    ;;
  backend)
    TARGET="${BACKEND_RELEASE_DIR}/${2:?请指定jar文件名，用 list 查看}"
    [ -f "$TARGET" ] || { echo "找不到 $TARGET"; exit 1; }
    ln -sfn "$TARGET" "$BACKEND_LINK"
    sudo systemctl restart "$SERVICE_NAME"
    echo "已回滚到: $TARGET"
    ;;
  frontend)
    TARGET="${FRONTEND_RELEASES_DIR}/${2:?请指定release目录名，用 list 查看}"
    [ -d "$TARGET" ] || { echo "找不到 $TARGET"; exit 1; }
    ln -sfn "$TARGET" "$FRONTEND_LINK"
    echo "已回滚到: $TARGET"
    ;;
  *)
    echo "用法: $0 {list|backend <jar名>|frontend <目录名>}"
    exit 1
    ;;
esac
