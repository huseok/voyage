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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=deploy/aliyun/_common.sh
source "$SCRIPT_DIR/_common.sh"

# 快速步骤失败时打印可复制的「全量」命令（依赖 RELEASE_VERBOSE / --frontend-clean / --backend-full 等）
print_full_retry_commands() {
  [[ "${RELEASE_SUPPRESS_RETRY_HINT:-}" == "1" ]] && return 0
  local vr="$VOYAGE_REPO"
  echo "" >&2
  echo "[release] ---------- 以下为「全量重试」示例，按需复制一行执行 ----------" >&2
  cat >&2 <<EOF

RELEASE_VERBOSE=1 bash $vr/deploy/aliyun/release.sh --frontend-clean --backend-full

# 仅前端：强制每次先 npm ci（不信任现有 node_modules 时）：
bash $vr/deploy/aliyun/release.sh --frontend-only --frontend-ci-first

# 仅前端（无人值守/CI）：先试 build，失败则自动 npm ci：
RELEASE_FRONTEND_AUTO_CI_ON_BUILD_FAIL=1 bash $vr/deploy/aliyun/release.sh --frontend-only

# 仅前端：启用 React Compiler 买家侧（scope 1；默认发版为 0 关闭）：
# RELEASE_FRONTEND_COMPILER_SCOPE=1 bash $vr/deploy/aliyun/release.sh --frontend-only

# 仅前端全量（删 node_modules + dist 再装）：
RELEASE_VERBOSE=1 bash $vr/deploy/aliyun/release.sh --frontend-only --frontend-clean

# 仅后端：Docker 镜像无缓存重建
bash $vr/deploy/aliyun/release.sh --backend-only --backend-full

# 仅后端：改容器内 Gradle（宿主 ./gradlew 失败时再试）
bash $vr/deploy/aliyun/release.sh --backend-only --backend-standard

# 若使用 /opt 入口（路径按你机器调整）：
# RELEASE_VERBOSE=1 /opt/publish-frontend.sh --frontend-clean
# RELEASE_GRADLE_MAX_HEAP=768m /opt/publish-backend-quick.sh
# /opt/publish-backend-full.sh --no-pull
EOF
  echo "[release] ---------------------------------------------------------------" >&2
  echo "" >&2
}

# 交互终端：询问是否执行降级；非交互：默认自动降级（可用 *_NO_AUTO_* 环境变量关闭）
release_wants_fallback() {
  local prompt="$1"
  local block_auto_env="${2:-}"
  if [[ -t 0 ]]; then
    read -r -p "[release] ${prompt} [y/N] " _yn || true
    [[ "${_yn:-}" =~ ^[yY](es)?$ ]]
    return $?
  fi
  if [[ -n "$block_auto_env" ]]; then
    case "$block_auto_env" in
      *[!a-zA-Z0-9_]*) die "release_wants_fallback: 非法环境变量名" ;;
    esac
    eval "local _blk=\${${block_auto_env}:-}"
    if [[ "${_blk:-}" == "1" ]]; then
      log "非交互环境且 ${block_auto_env}=1，不自动执行降级步骤"
      return 1
    fi
  fi
  log "非交互终端：自动执行降级步骤"
  return 0
}

# quick 模式宿主 Gradle 失败后是否改 standard：交互 [y/N] 默认否；非交互默认否（仅 RELEASE_BACKEND_QUICK_FAIL_AUTO_STANDARD=1 时自动改）。
release_wants_standard_after_quick_fail() {
  if [[ -t 0 ]]; then
    read -r -p "[release] 是否改用容器内 Gradle（standard）构建镜像？耗时更长但可不依赖宿主编译。 [y/N] " _yn || true
    [[ "${_yn:-}" =~ ^[yY](es)?$ ]]
    return $?
  fi
  if [[ "${RELEASE_BACKEND_QUICK_FAIL_AUTO_STANDARD:-}" == "1" ]]; then
    log "非交互环境且 RELEASE_BACKEND_QUICK_FAIL_AUTO_STANDARD=1，自动改用 standard"
    return 0
  fi
  log "非交互环境：默认不自动改 standard（需显式 RELEASE_BACKEND_QUICK_FAIL_AUTO_STANDARD=1）"
  return 1
}

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
FRONTEND_CLEAN=0
FRONTEND_CI_FIRST="${FRONTEND_CI_FIRST:-0}"
# 前端 React Compiler 范围：0=关闭 1=仅非 admin 2=含 admin（传给 foreign-trade-shop 的 REACT_COMPILER_SCOPE）。小机默认 0。
RELEASE_FRONTEND_COMPILER_SCOPE="${RELEASE_FRONTEND_COMPILER_SCOPE:-0}"
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
  --frontend-clean    全量前端：删除 node_modules 与 dist，再 npm ci + build（依赖/锁异常时用）
  --frontend-ci-first 前端始终先 npm ci 再 build（默认改为先试 build；非交互下失败后须 RELEASE_FRONTEND_AUTO_CI_ON_BUILD_FAIL=1 才会自动 ci）
  （默认）            后端 quick；前端先试 build；未装 JDK 请 --backend-standard 或 BACKEND_BUILD_MODE=standard

环境变量（可选）：
  GLOBUY_ROOT   默认 /opt/globuy
  VOYAGE_REPO   默认 $GLOBUY_ROOT/repo/voyage
  FRONTEND_REPO 默认 $GLOBUY_ROOT/repo/foreign-trade-shop
  ENV_FILE      默认 $GLOBUY_ROOT/config/env.backend
  WWW_FRONTEND  默认 $GLOBUY_ROOT/www/frontend
  COMPOSE_API_SERVICE  compose 里 API 服务名，默认 voyage-api
  BACKEND_BUILD_MODE   standard | quick | full（默认 quick）；三种 --backend-* 勿混用
  RELEASE_USE_LEGACY_COMPOSE  设为 1 时才允许走 Python docker-compose 1.x（不推荐）
  RELEASE_VERBOSE             设为 1 时 npm ci 使用 --loglevel=info（便于确认是否在拉依赖而非死机）
  RELEASE_SUPPRESS_RETRY_HINT 设为 1 时失败不打印下方「全量重试」复制块
  FRONTEND_CI_FIRST           设为 1 等价于 --frontend-ci-first（始终先 npm ci）
  RELEASE_FRONTEND_AUTO_CI_ON_BUILD_FAIL  非交互（无 TTY）下 build 失败后才自动 npm ci：必须设为 1 才会自动 ci（默认不自动，需你在交互终端选 y 或手动 ci）
  RELEASE_FRONTEND_COMPILER_SCOPE  前端 React Compiler：0=关闭 1=仅买家端（排除 src/admin）2=全 src 含后台。默认 0；详见 foreign-trade-shop vite.config.ts
  RELEASE_FRONTEND_NICE_BUILD       设为 1：前端 npm run build 使用 nice -n 15，减轻抢满 CPU（墙钟时间略增）
  RELEASE_GIT_PULL_NO_AUTO_SKIP    设为 1：非交互下 git pull 失败后将不再自动「跳过 pull 继续」
  RELEASE_BACKEND_QUICK_FAIL_AUTO_STANDARD  设为 1：非交互下宿主 Gradle 失败后才自动改 standard（默认不自动，等同 [y/N] 选 N）
  RELEASE_GRADLE_MAX_HEAP         宿主 Gradle 堆上限；≤3GB 内存机未设置时脚本默认 768m
  RELEASE_AUTO_STOP_API_ON_LOW_MEM  默认 1：小内存机发版前自动停 API（等效 --stop-api-first）
  RELEASE_DOCKER_FAIL_NO_AUTO_FULL 设为 1：Docker 失败后不自动 --no-cache 重建
  RELEASE_RSYNC_NO_AUTO_RETRY      设为 1：rsync 失败后不自动重试一次
  RELEASE_NGINX_FAIL_NO_SKIP       设为 1：nginx 失败后不提示忽略（交互仍可选取消）
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
    --frontend-clean) FRONTEND_CLEAN=1 ;;
    --frontend-ci-first) FRONTEND_CI_FIRST=1 ;;
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
  release_git_pull_repo() {
    local repo="$1"
    local label="$2"
    log "git pull ${label}: $repo"
    if git -C "$repo" pull --ff-only; then
      return 0
    fi
    log "git pull 失败（${label}）"
    if [[ -t 0 ]]; then
      read -r -p "[release] 是否重试一次 git pull？[Y/n] " _r || true
      if [[ ! "${_r:-}" =~ ^[nN](o)?$ ]]; then
        git -C "$repo" pull --ff-only && return 0
      fi
    else
      log "非交互：自动重试 pull 一次"
      sleep 1
      git -C "$repo" pull --ff-only && return 0
    fi
    if release_wants_fallback "是否跳过本次 pull，使用服务器当前代码继续发布？" "RELEASE_GIT_PULL_NO_AUTO_SKIP"; then
      log "已跳过 ${label} 的 pull，继续后续步骤"
      return 0
    fi
    print_full_retry_commands
    exit 1
  }

  if [[ "$DO_BACKEND" -eq 1 ]]; then
    release_git_pull_repo "$VOYAGE_REPO" "后端"
  fi
  if [[ "$DO_FRONTEND" -eq 1 ]]; then
    [[ -d "$FRONTEND_REPO" ]] || die "前端目录不存在: $FRONTEND_REPO"
    release_git_pull_repo "$FRONTEND_REPO" "前端"
  fi
fi

if [[ "$DO_FRONTEND" -eq 1 ]]; then
  [[ -d "$FRONTEND_REPO" ]] || die "前端目录不存在: $FRONTEND_REPO"
  command -v npm >/dev/null || die "未找到 npm，请先安装 Node.js ≥ 20"
  mkdir -p "$WWW_FRONTEND"
  case "${RELEASE_FRONTEND_COMPILER_SCOPE}" in
    0|1|2) ;;
    *) die "RELEASE_FRONTEND_COMPILER_SCOPE 必须是 0、1 或 2，当前: ${RELEASE_FRONTEND_COMPILER_SCOPE}" ;;
  esac
  export REACT_COMPILER_SCOPE="${RELEASE_FRONTEND_COMPILER_SCOPE}"
  log "前端 React Compiler：REACT_COMPILER_SCOPE=${REACT_COMPILER_SCOPE}（0=关 1=仅非 admin 2=含 admin）"
  if [[ "$FRONTEND_CLEAN" -eq 1 ]]; then
    log "前端全量：移除 node_modules 与 dist 后重装依赖"
    rm -rf "$FRONTEND_REPO/node_modules" "$FRONTEND_REPO/dist"
  fi
  _npm_flags=(ci --no-audit --fund=false)
  [[ "${RELEASE_VERBOSE:-}" == "1" ]] && _npm_flags+=(--loglevel=info)

  _frontend_npm_run_build() {
    if [[ "${RELEASE_FRONTEND_NICE_BUILD:-0}" == "1" ]] && command -v nice >/dev/null 2>&1; then
      nice -n 15 npm run build
    else
      npm run build
    fi
  }

  frontend_npm_ci_then_build() {
    log "前端 npm ci + npm run build（可加 RELEASE_VERBOSE=1 看进度）"
    if ! ( cd "$FRONTEND_REPO" && npm "${_npm_flags[@]}" && _frontend_npm_run_build ); then
      print_full_retry_commands
      exit 1
    fi
  }

  frontend_try_build_then_maybe_ci() {
    log "前端优先仅 npm run build（未加 --frontend-ci-first / FRONTEND_CI_FIRST / --frontend-clean 时不会先 npm ci）"
    if ( cd "$FRONTEND_REPO" && _frontend_npm_run_build ); then
      return 0
    fi
    log "npm run build 失败（常见：缺 node_modules、package-lock 更新后未同步依赖）"
    if [[ -t 0 ]]; then
      read -r -p "[release] 是否执行 npm ci 重装依赖后再 build？[y/N] " _yn || true
      if [[ ! "${_yn:-}" =~ ^[yY](es)?$ ]]; then
        log "已跳过 npm ci。可加 --frontend-ci-first，或在前端目录手动 npm ci 后重试"
        print_full_retry_commands
        exit 1
      fi
      log "将执行 npm ci 后再次 build"
    else
      if [[ "${RELEASE_FRONTEND_AUTO_CI_ON_BUILD_FAIL:-}" != "1" ]]; then
        log "非交互环境：默认不在失败后自动 npm ci。请用 ssh -t 登录后重试以便手动选 y，或设置 RELEASE_FRONTEND_AUTO_CI_ON_BUILD_FAIL=1 后再跑本脚本"
        print_full_retry_commands
        exit 1
      fi
      log "非交互环境：RELEASE_FRONTEND_AUTO_CI_ON_BUILD_FAIL=1，自动执行 npm ci 后重试 build"
    fi
    frontend_npm_ci_then_build
  }

  if [[ "$FRONTEND_CLEAN" -eq 1 ]] || [[ "$FRONTEND_CI_FIRST" -eq 1 ]]; then
    frontend_npm_ci_then_build
  else
    frontend_try_build_then_maybe_ci
  fi

  log "同步静态资源 → $WWW_FRONTEND"
  if ! rsync -a --delete "$FRONTEND_REPO/dist/" "$WWW_FRONTEND/"; then
    log "rsync 同步失败"
    if release_wants_fallback "是否重试一次 rsync？" "RELEASE_RSYNC_NO_AUTO_RETRY"; then
      rsync -a --delete "$FRONTEND_REPO/dist/" "$WWW_FRONTEND/" || {
        print_full_retry_commands
        exit 1
      }
    else
      print_full_retry_commands
      exit 1
    fi
  fi
fi

if [[ "$DO_BACKEND" -eq 1 ]]; then
  # 2GB 等小内存机：构建前默认停 API，把内存让给 Gradle（可用 RELEASE_AUTO_STOP_API_ON_LOW_MEM=0 关闭）
  if [[ "$STOP_API_BEFORE_BUILD" -eq 0 ]] && [[ "${RELEASE_AUTO_STOP_API_ON_LOW_MEM:-1}" == "1" ]] && globuy_is_low_memory_server; then
    STOP_API_BEFORE_BUILD=1
    log "小内存机默认构建前停止 API（等效 --stop-api-first；RELEASE_AUTO_STOP_API_ON_LOW_MEM=0 可关闭）"
  fi

  compose_files=(-f "$COMPOSE_REL")
  if [[ "$BACKEND_BUILD_MODE" == quick ]]; then
    compose_files+=(-f "$COMPOSE_QUICK_REL")
  fi

  if [[ "$BACKEND_BUILD_MODE" == full ]] && globuy_is_low_memory_server; then
    log "警告: 约 2GB 内存不建议 --backend-full（容器内无缓存 Gradle 极易 OOM）；优先 globuy-backend-quick 或 globuy-compile-quick"
  fi

  case "$BACKEND_BUILD_MODE" in
    standard)
      log "后端构建模式：standard（容器内 Gradle；源码变更会使镜像层失效，往往每次整编，较慢但无需宿主 JDK）"
      ;;
    quick)
      log "后端构建模式：quick（宿主 ./gradlew bootJar，镜像仅 COPY JAR，通常明显更快）"
      if ! globuy_host_gradle_boot_jar 0; then
        log "宿主 Gradle 失败"
        if release_wants_standard_after_quick_fail; then
          BACKEND_BUILD_MODE=standard
          compose_files=(-f "$COMPOSE_REL")
          log "已切换为 standard，将在 Docker 镜像构建阶段执行 Gradle"
        else
          print_full_retry_commands
          exit 1
        fi
      fi
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

  backend_docker_up() {
    local force_nocache="$1"
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
      log "清理名称含 globuy-api 的旧容器（避免 already in use）"
      docker ps -aq --filter name=globuy-api | xargs -r docker rm -f 2>/dev/null || true
      if [[ "$force_nocache" -eq 1 ]]; then
        docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" build --no-cache "$COMPOSE_API_SERVICE"
        docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" up -d
      elif [[ "$BACKEND_BUILD_MODE" == full ]]; then
        docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" build --no-cache "$COMPOSE_API_SERVICE"
        docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" up -d
      else
        docker_compose "${compose_files[@]}" --env-file "$ENV_FILE" up -d --build
      fi
    )
  }

  if ! backend_docker_up 0; then
    log "Docker 构建或启动失败"
    if release_wants_fallback "是否对 voyage-api 执行无缓存重建（docker build --no-cache）后再启动？" "RELEASE_DOCKER_FAIL_NO_AUTO_FULL"; then
      backend_docker_up 1 || {
        print_full_retry_commands
        exit 1
      }
    else
      print_full_retry_commands
      exit 1
    fi
  fi
  log "后端健康检查（本地 8080，可按需改 API_PORT）"
  sleep 2
  curl -fsS "http://127.0.0.1:${API_PORT:-8080}/api/v1/tags" >/dev/null && log "GET /api/v1/tags 正常" || log "警告: /api/v1/tags 未响应，请 docker compose logs $COMPOSE_API_SERVICE 自查"
fi

if [[ "$DO_NGINX" -eq 1 ]]; then
  if command -v nginx >/dev/null 2>&1; then
    log "nginx -t && reload"
    if ! ( sudo nginx -t && sudo systemctl reload nginx ); then
      log "nginx 校验或 reload 失败"
      if release_wants_fallback "是否忽略 nginx 错误并结束发布（请稍后手动 nginx -t / reload）？" "RELEASE_NGINX_FAIL_NO_SKIP"; then
        log "已按你的选择忽略 nginx 错误"
      else
        exit 1
      fi
    fi
  else
    log "未安装 nginx，跳过 reload"
  fi
fi

log "完成。"
