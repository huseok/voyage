#!/usr/bin/env bash
# Globuy 一键停止：Nginx（前端静态站点）+ Docker Compose 栈（Postgres + 后端 API）
# 仅停止容器，不删除镜像与数据卷；下次用 restart.sh 或 docker compose up -d 即可恢复。
#
# 用法（服务器）：
#   chmod +x /opt/globuy/repo/voyage/deploy/aliyun/stop.sh
#   bash /opt/globuy/repo/voyage/deploy/aliyun/stop.sh
# 任意目录：bash /opt/globuy/repo/voyage/deploy/publish-stop.sh
#
# 选项见 --help。环境变量 GLOBUY_ROOT / VOYAGE_REPO / ENV_FILE 可覆盖默认路径。
set -euo pipefail

# shellcheck source=deploy/aliyun/_common.sh
source "${VOYAGE_REPO:-/opt/globuy/repo/voyage}/deploy/aliyun/_common.sh"
SCRIPT_DIR="$(globuy_resolve_script_dir "${BASH_SOURCE[0]}")"

DO_FRONTEND=1
DO_BACKEND=1
BACKEND_SCOPE="all"

usage() {
  cat <<'EOF'
Globuy 一键停止（前端 Nginx + 后端 Docker）

选项：
  --frontend-only   只停止 Nginx（对外不再提供静态页与反代）
  --backend-only    只停止 Docker 栈（默认停 db + voyage-api）
  --api-only        与 --backend-only 合用：仅停 API 容器，保留 Postgres
  -h, --help        显示本帮助

环境变量（可选）：
  GLOBUY_ROOT / VOYAGE_REPO / ENV_FILE / COMPOSE_API_SERVICE / COMPOSE_DB_SERVICE
  RELEASE_USE_LEGACY_COMPOSE  设为 1 时才允许 Python docker-compose 1.x（不推荐）
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --frontend-only) DO_BACKEND=0 ;;
    --backend-only) DO_FRONTEND=0 ;;
    --api-only) BACKEND_SCOPE="api" ;;
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
      log "停止后端 API 容器: $COMPOSE_API_SERVICE"
      docker_compose "${COMPOSE_FILES[@]}" --env-file "$ENV_FILE" stop "$COMPOSE_API_SERVICE" || true
    else
      log "停止 Docker 栈（$COMPOSE_DB_SERVICE + $COMPOSE_API_SERVICE）"
      docker_compose "${COMPOSE_FILES[@]}" --env-file "$ENV_FILE" stop || true
    fi
  )
fi

if [[ "$DO_FRONTEND" -eq 1 ]]; then
  if globuy_nginx_available; then
    log "停止 Nginx（前端静态与 /api 反代）"
    if sudo systemctl stop nginx; then
      log "Nginx 已停止"
    else
      die "systemctl stop nginx 失败（请检查 sudo 权限或服务状态）"
    fi
  else
    log "未安装 nginx，跳过前端停止"
  fi
fi

log "停止完成。恢复请执行: bash $VOYAGE_REPO/deploy/aliyun/restart.sh"
