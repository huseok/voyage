#!/usr/bin/env bash
# deploy/aliyun 脚本共用：日志、路径默认值、Docker Compose V2 探测。
# 由 stop.sh / restart.sh / release.sh 等 source，勿直接执行。

log() { printf '[globuy] %s\n' "$*"; }
die() { echo "[globuy] 错误: $*" >&2; exit 1; }

# 优先 Compose V2；仅当 RELEASE_USE_LEGACY_COMPOSE=1 时才允许 Python docker-compose 1.x。
docker_compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    local dc_short dc_line
    dc_short=$(docker-compose version --short 2>/dev/null || true)
    dc_line=$(docker-compose version 2>/dev/null | head -n1 || true)
    if [[ "$dc_short" =~ ^2\. ]] || [[ "$dc_line" =~ docker-compose\ version\ v?2\. ]]; then
      docker-compose "$@"
      return
    fi
    if [[ "${RELEASE_USE_LEGACY_COMPOSE:-}" == "1" ]]; then
      log "警告: RELEASE_USE_LEGACY_COMPOSE=1，使用旧版 docker-compose（若报错 ContainerConfig 请装 docker-compose-plugin）" >&2
      docker-compose "$@"
      return
    fi
  fi
  die "未检测到 Docker Compose V2。请安装 docker-compose-plugin 后执行 docker compose version 确认。"
}

GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
ENV_FILE="${ENV_FILE:-$GLOBUY_ROOT/config/env.backend}"
COMPOSE_REL="deploy/aliyun/docker-compose.stack.yml"
COMPOSE_API_SERVICE="${COMPOSE_API_SERVICE:-voyage-api}"
COMPOSE_DB_SERVICE="${COMPOSE_DB_SERVICE:-db}"

globuy_require_env_file() {
  [[ -f "$ENV_FILE" ]] || die "缺少环境文件: $ENV_FILE（首次部署请复制 env.backend.example）"
}

globuy_compose_files() {
  COMPOSE_FILES=(-f "$COMPOSE_REL")
}

globuy_nginx_available() {
  command -v nginx >/dev/null 2>&1
}
