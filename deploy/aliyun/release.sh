#!/usr/bin/env bash
# Globuy 阿里云单机一键发版：拉代码（可选）→ 前端 build → 后端 Docker 重建 → Nginx 校验并重载（可选）
# 后端构建模式（默认 quick）：见 --backend-standard / --backend-full / --help
# 可选 --stop-api-first：构建前先 stop voyage-api（默认不停）
# 用法（在服务器上）：
#   chmod +x /opt/globuy/repo/voyage/deploy/aliyun/release.sh
#   /opt/globuy/repo/voyage/deploy/aliyun/release.sh
# 环境变量可覆盖默认路径，见脚本内 GLOBUY_ROOT / VOYAGE_REPO / FRONTEND_REPO 等。
set -euo pipefail

log() { printf '[release] %s\n' "$*"; }
die() { echo "[release] 错误: $*" >&2; exit 1; }

docker_compose() {
  # Compose V2（docker compose）与新版 Docker 配套。Ubuntu 自带 Python **docker-compose 1.29** 会 KeyError: ContainerConfig，禁止默认使用。
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
  die "未检测到 Docker Compose V2。请安装：sudo apt-get update && sudo apt-get install -y docker-compose-plugin ，然后执行 docker compose version 确认。勿再用 Python docker-compose 1.x。"
}

GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
FRONTEND_REPO="${FRONTEND_REPO:-$GLOBUY_ROOT/repo/foreign-trade-shop}"
ENV_FILE="${ENV_FILE:-$GLOBUY_ROOT/config/env.backend}"
WWW_FRONTEND="${WWW_FRONTEND:-$GLOBUY_ROOT/www/frontend}"
COMPOSE_REL="deploy/aliyun/docker-compose.stack.yml"
COMPOSE_QUICK_REL="deploy/aliyun/docker-compose.stack.quick.yml"
COMPOSE_API_SERVICE="${COMPOSE_API_SERVICE:-voyage-api}"

DO_PULL=1
DO_FRONTEND=1
DO_BACKEND=1
DO_NGINX=1
STOP_API_BEFORE_BUILD=0
# 后端镜像构建：quick=默认，宿主 Gradle + Dockerfile.fast（需 JDK）；standard=容器内 Gradle；full=docker --no-cache
BACKEND_BUILD_MODE="${BACKEND_BUILD_MODE:-quick}"
BACKEND_CLI_FLAG=""

usage() {
  cat <<'EOF'
Globuy 一键发版脚本（详见 deploy/aliyun/DEPLOY_STEP_BY_STEP.md）

选项：
  --no-pull           跳过 git pull（前后端都不拉）
  --frontend-only     只构建并同步前端静态资源
  --backend-only      只重建并启动后端 Docker 栈
  --no-nginx          不执行 nginx -t / reload（需手动 reload）
  --stop-api-first    构建前先停止运行中的 API 容器（默认不停）；减轻构建期资源争抢，停机时间会变长
  --backend-standard  标准后端：容器内 Gradle（无需宿主 JDK，但每次多半整编较慢）
  --backend-quick     快速后端（默认）：宿主 ./gradlew bootJar + Dockerfile.fast（需 JDK 17+）
  --backend-full      全量后端：docker build --no-cache（依赖/镜像缓存异常时用）
  （默认）            quick；未装 JDK 时请用 --backend-standard 或 BACKEND_BUILD_MODE=standard

环境变量（可选）：
  GLOBUY_ROOT   默认 /opt/globuy
  VOYAGE_REPO   默认 $GLOBUY_ROOT/repo/voyage
  FRONTEND_REPO 默认 $GLOBUY_ROOT/repo/foreign-trade-shop
  ENV_FILE      默认 $GLOBUY_ROOT/config/env.backend
  WWW_FRONTEND  默认 $GLOBUY_ROOT/www/frontend
  COMPOSE_API_SERVICE  compose 里 API 服务名，默认 voyage-api
  BACKEND_BUILD_MODE   standard | quick | full（默认 quick）；三种 --backend-* 勿混用
  RELEASE_USE_LEGACY_COMPOSE  设为 1 时才允许走 Python docker-compose 1.x（不推荐）
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --no-pull) DO_PULL=0 ;;
    --frontend-only) DO_BACKEND=0; DO_NGINX=0 ;;
    --backend-only) DO_FRONTEND=0 ;;
    --no-nginx) DO_NGINX=0 ;;
    --stop-api-first) STOP_API_BEFORE_BUILD=1 ;;
    --backend-standard)
      [[ -z "$BACKEND_CLI_FLAG" ]] || die "不要同时使用多种 --backend-standard / --backend-quick / --backend-full"
      BACKEND_CLI_FLAG=standard
      ;;
    --backend-quick)
      [[ -z "$BACKEND_CLI_FLAG" ]] || die "不要同时使用多种 --backend-standard / --backend-quick / --backend-full"
      BACKEND_CLI_FLAG=quick
      ;;
    --backend-full)
      [[ -z "$BACKEND_CLI_FLAG" ]] || die "不要同时使用多种 --backend-standard / --backend-quick / --backend-full"
      BACKEND_CLI_FLAG=full
      ;;
    *) echo "未知参数: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

[[ -n "$BACKEND_CLI_FLAG" ]] && BACKEND_BUILD_MODE="$BACKEND_CLI_FLAG"

if [[ "$BACKEND_BUILD_MODE" != standard && "$BACKEND_BUILD_MODE" != quick && "$BACKEND_BUILD_MODE" != full ]]; then
  die "BACKEND_BUILD_MODE 必须是 standard | quick | full，当前: $BACKEND_BUILD_MODE"
fi

require_file() { [[ -f "$1" ]] || die "缺少文件: $1（首次部署请复制 env.backend.example 并编辑）"; }

[[ -d "$VOYAGE_REPO" ]] || die "后端目录不存在: $VOYAGE_REPO"
require_file "$ENV_FILE"

if [[ "$DO_PULL" -eq 1 ]]; then
  if [[ "$DO_BACKEND" -eq 1 ]]; then
    log "git pull 后端: $VOYAGE_REPO"
    git -C "$VOYAGE_REPO" pull --ff-only
  fi
  if [[ "$DO_FRONTEND" -eq 1 ]]; then
    [[ -d "$FRONTEND_REPO" ]] || die "前端目录不存在: $FRONTEND_REPO"
    log "git pull 前端: $FRONTEND_REPO"
    git -C "$FRONTEND_REPO" pull --ff-only
  fi
fi

if [[ "$DO_FRONTEND" -eq 1 ]]; then
  [[ -d "$FRONTEND_REPO" ]] || die "前端目录不存在: $FRONTEND_REPO"
  command -v npm >/dev/null || die "未找到 npm，请先安装 Node.js ≥ 20"
  mkdir -p "$WWW_FRONTEND"
  log "前端 npm ci + build"
  ( cd "$FRONTEND_REPO" && npm ci && npm run build )
  log "同步静态资源 → $WWW_FRONTEND"
  rsync -a --delete "$FRONTEND_REPO/dist/" "$WWW_FRONTEND/"
fi

if [[ "$DO_BACKEND" -eq 1 ]]; then
  compose_files=(-f "$COMPOSE_REL")
  if [[ "$BACKEND_BUILD_MODE" == quick ]]; then
    compose_files+=(-f "$COMPOSE_QUICK_REL")
  fi

  case "$BACKEND_BUILD_MODE" in
    standard)
      log "后端构建模式：standard（容器内 Gradle；源码变更会使镜像层失效，往往每次整编，较慢但无需宿主 JDK）"
      ;;
    quick)
      command -v java >/dev/null || die "quick 模式（默认）需要服务器已安装 JDK（建议 17）；未装 JDK 请执行：--backend-standard 或 BACKEND_BUILD_MODE=standard"
      log "后端构建模式：quick（宿主 ./gradlew bootJar，镜像仅 COPY JAR，通常明显更快）"
      (
        cd "$VOYAGE_REPO"
        chmod +x ./gradlew 2>/dev/null || true
        ./gradlew bootJar -x test
      )
      ;;
    full)
      log "后端构建模式：full（docker --no-cache，全量重建镜像层，最慢；依赖或 Docker 缓存异常时用）"
      ;;
  esac

  if [[ "$BACKEND_BUILD_MODE" == standard ]]; then
    log "Docker 重建并后台启动（standard：容器内 Gradle，关闭 BuildKit）"
  else
    log "Docker 重建并后台启动"
  fi
  (
    cd "$VOYAGE_REPO"
    if [[ "$BACKEND_BUILD_MODE" == standard ]]; then
      unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
      export DOCKER_BUILDKIT=0
    fi
    if [[ "$STOP_API_BEFORE_BUILD" -eq 1 ]]; then
      log "先停止 $COMPOSE_API_SERVICE（减轻本机构建期争抢；至 up --build 完成前 API 不可用）"
      docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" stop "$COMPOSE_API_SERVICE" || true
    fi
    # 旧 Compose V1 失败重建常留下「项目ID_globuy-api」，与 compose 里 container_name: globuy-api 冲突
    log "清理名称含 globuy-api 的旧容器（避免 already in use）"
    docker ps -aq --filter name=globuy-api | xargs -r docker rm -f 2>/dev/null || true
    if [[ "$BACKEND_BUILD_MODE" == full ]]; then
      docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" build --no-cache "$COMPOSE_API_SERVICE"
      docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" up -d
    else
      docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" up -d --build
    fi
  )
  log "后端健康检查（本地 8080，可按需改 API_PORT）"
  sleep 2
  curl -fsS "http://127.0.0.1:${API_PORT:-8080}/api/v1/tags" >/dev/null && log "GET /api/v1/tags 正常" || log "警告: /api/v1/tags 未响应，请 docker compose logs $COMPOSE_API_SERVICE 自查"
fi

if [[ "$DO_NGINX" -eq 1 ]]; then
  if command -v nginx >/dev/null 2>&1; then
    log "nginx -t && reload"
    sudo nginx -t && sudo systemctl reload nginx
  else
    log "未安装 nginx，跳过 reload"
  fi
fi

log "完成。"
