#!/usr/bin/env bash
# Globuy 一键重启：Docker Compose 栈（Postgres + API）+ Nginx（前端静态与反代）
# 不拉代码、不构建镜像；仅启动已有容器并重载 Nginx。发版请用 release.sh。
#
# 用法（服务器）：
#   chmod +x /opt/globuy/repo/voyage/deploy/aliyun/restart.sh
#   bash /opt/globuy/repo/voyage/deploy/aliyun/restart.sh
# 任意目录：bash /opt/globuy/repo/voyage/deploy/publish-restart.sh
#
# 选项见 --help。
set -euo pipefail

# shellcheck source=deploy/aliyun/_common.sh
source "${VOYAGE_REPO:-/opt/globuy/repo/voyage}/deploy/aliyun/_common.sh"
SCRIPT_DIR="$(globuy_resolve_script_dir "${BASH_SOURCE[0]}")"

DO_FRONTEND=1
DO_BACKEND=1
BACKEND_SCOPE="all"
DO_HEALTH_CHECK=1

usage() {
  cat <<'EOF'
Globuy 一键重启（不重新 build，仅恢复运行）

选项：
  --frontend-only     只重启 Nginx（start 或 reload）
  --backend-only      只启动/重启 Docker 栈
  --api-only          与 --backend-only 合用：仅重启 API，不动 Postgres
  --no-health-check   跳过后端 curl /api/v1/tags 探测
  -h, --help          显示本帮助

环境变量（可选）：
  GLOBUY_ROOT / VOYAGE_REPO / ENV_FILE / API_PORT（默认 8080）
  COMPOSE_API_SERVICE / COMPOSE_DB_SERVICE
  RELEASE_USE_LEGACY_COMPOSE  设为 1 时才允许 Python docker-compose 1.x（不推荐）
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --frontend-only) DO_BACKEND=0 ;;
    --backend-only) DO_FRONTEND=0 ;;
    --api-only) BACKEND_SCOPE="api" ;;
    --no-health-check) DO_HEALTH_CHECK=0 ;;
    *) echo "未知参数: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

if [[ "$DO_FRONTEND" -eq 0 && "$DO_BACKEND" -eq 0 ]]; then
  die "请至少保留前端或后端其一（勿同时使用 --frontend-only 与 --backend-only）"
fi

if [[ "$DO_BACKEND" -eq 1 ]]; then
  globuy_require_env_file
  globuy_compose_files
  (
    cd "$VOYAGE_REPO"
    if [[ "$BACKEND_SCOPE" == "api" ]]; then
      log "重启后端 API: $COMPOSE_API_SERVICE"
      docker_compose "${COMPOSE_FILES[@]}" --env-file "$ENV_FILE" up -d "$COMPOSE_API_SERVICE"
    else
      log "启动 Docker 栈（$COMPOSE_DB_SERVICE + $COMPOSE_API_SERVICE）"
      docker_compose "${COMPOSE_FILES[@]}" --env-file "$ENV_FILE" up -d
    fi
  )
  if [[ "$DO_HEALTH_CHECK" -eq 1 ]]; then
    log "等待 API 就绪（最多约 60s）"
    api_port="${API_PORT:-8080}"
    ok=0
    for _ in $(seq 1 30); do
      if curl -fsS "http://127.0.0.1:${api_port}/api/v1/tags" >/dev/null 2>&1; then
        ok=1
        break
      fi
      sleep 2
    done
    if [[ "$ok" -eq 1 ]]; then
      log "GET /api/v1/tags 正常"
    else
      log "警告: /api/v1/tags 未响应，请执行: cd $VOYAGE_REPO && docker compose -f $COMPOSE_REL logs $COMPOSE_API_SERVICE"
    fi
  fi
fi

if [[ "$DO_FRONTEND" -eq 1 ]]; then
  if globuy_nginx_available; then
    log "校验并重载 Nginx"
    if sudo nginx -t && sudo systemctl reload nginx 2>/dev/null; then
      log "Nginx 已 reload"
    elif sudo nginx -t && sudo systemctl start nginx; then
      log "Nginx 已 start"
    else
      die "nginx -t 或 systemctl 操作失败"
    fi
  else
    log "未安装 nginx，跳过前端重启"
  fi
fi

log "重启完成。"
