#!/usr/bin/env bash
# 仅宿主 Gradle 全量编译（clean bootJar），不拉代码、不构建 Docker、不启服务。
# 依赖异常或增量编译结果可疑时使用；耗时明显长于 compile-backend-quick.sh。
#
# 用法：
#   bash deploy/aliyun/compile-backend-full.sh
#   RELEASE_GRADLE_MAX_HEAP=768m bash deploy/aliyun/compile-backend-full.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=deploy/aliyun/_common.sh
source "$SCRIPT_DIR/_common.sh"

usage() {
  cat <<'EOF'
宿主 Gradle 全量编译（clean bootJar，--no-daemon）

环境变量：
  VOYAGE_REPO / RELEASE_GRADLE_MAX_HEAP（2GB 小机建议 768m）

说明：仅清理并重新编译 JAR；若需 Docker 镜像无缓存重建，请用 build-backend-full.sh
EOF
}

[[ "${1:-}" == "-h" || "${1:-}" == "--help" ]] && { usage; exit 0; }
[[ $# -eq 0 ]] || die "未知参数: $*（本脚本无额外选项）"

globuy_host_gradle_boot_jar 1
log "全量编译完成，JAR 位于: $VOYAGE_REPO/build/libs/"
