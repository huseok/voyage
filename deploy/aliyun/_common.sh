#!/usr/bin/env bash
# deploy/aliyun 脚本共用：日志、路径默认值、Docker Compose V2 探测。
# 由 stop.sh / restart.sh / release.sh 等 source，勿直接执行。

# 解析脚本真实目录（globuy-release 等软链到 /usr/local/bin 时 BASH_SOURCE 会指向链接路径）。
globuy_resolve_script_dir() {
  local src="${1:?}"
  if command -v readlink >/dev/null 2>&1; then
    local resolved
    resolved=$(readlink -f "$src" 2>/dev/null || true)
    [[ -n "$resolved" ]] && src="$resolved"
  fi
  cd "$(dirname "$src")" && pwd
}

# 若调用方已定义 log/die（如 release.sh 用 [release] 前缀），则复用调用方实现。
if ! declare -f log >/dev/null 2>&1; then
  log() { printf '[globuy] %s\n' "$*" >&2; }
fi
if ! declare -f die >/dev/null 2>&1; then
  die() { echo "[globuy] 错误: $*" >&2; exit 1; }
fi

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
COMPOSE_QUICK_REL="deploy/aliyun/docker-compose.stack.quick.yml"
COMPOSE_API_SERVICE="${COMPOSE_API_SERVICE:-voyage-api}"
COMPOSE_DB_SERVICE="${COMPOSE_DB_SERVICE:-db}"

# 读取 Linux 总内存（KB）；非 Linux 或读失败返回 0。
globuy_mem_total_kb() {
  awk '/MemTotal:/ {print $2}' /proc/meminfo 2>/dev/null || echo 0
}

# 约 3GB 及以下视为小内存机（标称 2GB 的 VPS 通常落在此区间）。
globuy_is_low_memory_server() {
  local mem_kb
  mem_kb=$(globuy_mem_total_kb)
  [[ "$mem_kb" -gt 0 && "$mem_kb" -le 3145728 ]]
}

# 小内存机未显式配置时默认 Gradle 768m（覆盖 gradle.properties 的 1536m）。
globuy_apply_low_memory_defaults() {
  if ! globuy_is_low_memory_server; then
    return 0
  fi
  local mem_mb=$(( $(globuy_mem_total_kb) / 1024 ))
  if [[ -z "${RELEASE_GRADLE_MAX_HEAP:-}" ]]; then
    RELEASE_GRADLE_MAX_HEAP=768m
    export RELEASE_GRADLE_MAX_HEAP
    log "检测到约 ${mem_mb}MB 内存，默认 Gradle 堆 768m（export RELEASE_GRADLE_MAX_HEAP 可覆盖）"
  fi
  if [[ "${RELEASE_AUTO_STOP_API_ON_LOW_MEM:-1}" == "1" ]]; then
    log "小内存提示：发版前可先 globuy-stop，编译完成后再 globuy-restart，减少与 Docker 争抢内存"
  fi
}

# 宿主机构建 bootJar。full=1 时先 clean；自动停 Daemon，单进程 --no-daemon，减轻小机 OOM 假死。
globuy_host_gradle_boot_jar() {
  local full="${1:-0}"
  local gradle_extra=()
  command -v java >/dev/null || die "需要已安装 JDK（建议 17）；无 JDK 请用 build-backend-full.sh --backend-standard"
  globuy_apply_low_memory_defaults
  if [[ -n "${RELEASE_GRADLE_MAX_HEAP:-}" ]]; then
    gradle_extra+=("-Dorg.gradle.jvmargs=-Xmx${RELEASE_GRADLE_MAX_HEAP} -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true")
  fi
  (
    cd "$VOYAGE_REPO"
    chmod +x ./gradlew 2>/dev/null || true
    log "停止残留 Gradle Daemon"
    ./gradlew --stop >/dev/null 2>&1 || true
    if [[ "$full" -eq 1 ]]; then
      log "全量编译（宿主）: ./gradlew clean bootJar -x test --no-daemon"
      ./gradlew clean bootJar -x test --no-daemon --console=plain "${gradle_extra[@]}"
    else
      log "快速编译（宿主）: ./gradlew bootJar -x test --no-daemon（增量）"
      ./gradlew bootJar -x test --no-daemon --console=plain "${gradle_extra[@]}"
    fi
  )
}

globuy_require_env_file() {
  [[ -f "$ENV_FILE" ]] || die "缺少环境文件: $ENV_FILE（首次部署请复制 env.backend.example）"
}

globuy_compose_files() {
  COMPOSE_FILES=(-f "$COMPOSE_REL")
}

globuy_nginx_available() {
  command -v nginx >/dev/null 2>&1
}
