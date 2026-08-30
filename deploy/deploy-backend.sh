#!/bin/bash
# 后端部署脚本
# 使用方式: bash deploy-backend.sh <上传到服务器的jar文件路径>
# 例: bash deploy-backend.sh /tmp/deploy-incoming/smart-campus-backend-0.0.1-SNAPSHOT.jar
#
# 本脚本由 CI 从仓库 deploy/ 目录自动同步到 VPS（见 .github/workflows/deploy.yml），仓库为唯一真源。
set -euo pipefail

RELEASE_DIR="/opt/place-explore/backend/releases"
APP_JAR_LINK="/opt/place-explore/backend/app.jar"
SERVICE_NAME="place-explore-backend.service"
KEEP_RELEASES=5

# 健康检查（2026-08-29 事故复盘项）：management 端口只绑 localhost，
# /actuator/health 返回 200 才算部署成功。仅凭 systemctl is-active 查不出
# "进程活着但接口全 500" 的带病启动（当日缺列事故即此形态，坏版本被静默放行）。
# 健康检查不过 → 自动回滚软链到上一版并重启。
MANAGEMENT_HEALTH_URL="http://localhost:9091/actuator/health"
HEALTH_TIMEOUT_SECONDS=90
HEALTH_CHECK_INTERVAL=3

if [ $# -lt 1 ]; then
  echo "用法: $0 <新jar文件路径>"
  exit 1
fi

INCOMING_JAR="$1"
if [ ! -f "$INCOMING_JAR" ]; then
  echo "错误: 找不到文件 $INCOMING_JAR"
  exit 1
fi

# 当前运行版本 = 回滚目标（在切换软链之前记录；首次部署时为空，届时只报错不回滚）
PREV_JAR=$(readlink -f "$APP_JAR_LINK" 2>/dev/null || true)

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BASE_NAME=$(basename "$INCOMING_JAR" .jar)
NEW_JAR="${RELEASE_DIR}/${BASE_NAME}-${TIMESTAMP}.jar"

# 轮询健康端点直到就绪；进程退出或超时返回非零
wait_for_health() {
  local timeout=$1 elapsed=0
  until curl -fsS "$MANAGEMENT_HEALTH_URL" >/dev/null 2>&1; do
    if ! sudo systemctl is-active --quiet "$SERVICE_NAME"; then
      echo "==> 失败: 服务进程未处于 active 状态"
      return 1
    fi
    sleep "$HEALTH_CHECK_INTERVAL"
    elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
    if [ "$elapsed" -ge "$timeout" ]; then
      echo "==> 失败: 健康检查超时 ${timeout}s（$MANAGEMENT_HEALTH_URL）"
      return 1
    fi
  done
  return 0
}

fail_and_rollback() {
  echo "==> 部署失败，最近日志："
  echo "    sudo journalctl -u $SERVICE_NAME -n 50 --no-pager"
  if [ -n "$PREV_JAR" ] && [ -f "$PREV_JAR" ]; then
    echo "==> 自动回滚到上一版: $(basename "$PREV_JAR")"
    ln -sfn "$PREV_JAR" "$APP_JAR_LINK"
    sudo systemctl restart "$SERVICE_NAME"
    if wait_for_health "$HEALTH_TIMEOUT_SECONDS"; then
      echo "==> 回滚完成，线上已恢复运行上一版。请在本轮 CI 日志排查失败原因。"
    else
      echo "==> 严重: 回滚后健康检查仍未通过，请立即人工介入！"
    fi
  else
    echo "==> 无上一版可回滚（首次部署），请人工修复。"
  fi
  exit 1
}

echo "==> 拷贝新版本到 releases 目录"
cp "$INCOMING_JAR" "$NEW_JAR"

echo "==> 切换 app.jar 软链"
ln -sfn "$NEW_JAR" "$APP_JAR_LINK"

echo "==> 重启后端服务"
sudo systemctl restart "$SERVICE_NAME"

echo "==> 等待健康检查通过（最长 ${HEALTH_TIMEOUT_SECONDS}s）: $MANAGEMENT_HEALTH_URL"
wait_for_health "$HEALTH_TIMEOUT_SECONDS" || fail_and_rollback

echo "==> 部署成功: $(basename "$NEW_JAR")"

echo "==> 清理旧版本，只保留最近 ${KEEP_RELEASES} 个"
cd "$RELEASE_DIR"
ls -t | tail -n +$((KEEP_RELEASES + 1)) | xargs -r rm -f

echo "==> 完成。当前 releases 目录:"
ls -la "$RELEASE_DIR"
