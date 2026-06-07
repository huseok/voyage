#!/usr/bin/env bash
# 仅宿主 Gradle 快速编译（增量 bootJar），不拉代码、不构建 Docker、不启服务。
# 产出：$VOYAGE_REPO/build/libs/*.jar
#
# 用法：
#   bash deploy/aliyun/compile-backend-quick.sh
#   RELEASE_GRADLE_MAX_HEAP=768m bash deploy/aliyun/compile-backend-quick.sh
set -euo pipefail

# shellcheck source=deploy/aliyun/_common.sh
source "${VOYAGE_REPO:-/opt/globuy/repo/voyage}/deploy/aliyun/_common.sh"
SCRIPT_DIR="$(globuy_resolve_script_dir "${BASH_SOURCE[0]}")"

usage() {
  cat <<'EOF'
宿主 Gradle 快速编译（增量 bootJar，--no-daemon）

环境变量：
  VOYAGE_REPO / RELEASE_GRADLE_MAX_HEAP（2GB 小机建议 768m）
  RELEASE_USE_LEGACY_COMPOSE  与本脚本无关，仅其他 deploy 脚本使用

编译完成后可手动：
  docker compose -f deploy/aliyun/docker-compose.stack.yml \
    -f deploy/aliyun/docker-compose.stack.quick.yml \
    --env-file /opt/globuy/config/env.backend up -d --build
EOF
}

[[ "${1:-}" == "-h" || "${1:-}" == "--help" ]] && { usage; exit 0; }
[[ $# -eq 0 ]] || die "未知参数: $*（本脚本无额外选项，全量请用 compile-backend-full.sh）"

globuy_host_gradle_boot_jar 0
log "快速编译完成，JAR 位于: $VOYAGE_REPO/build/libs/"
