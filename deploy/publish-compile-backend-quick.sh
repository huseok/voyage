#!/usr/bin/env bash
# 任意目录：宿主 Gradle 快速编译（增量 bootJar）
# 可选：sudo ln -sf .../publish-compile-backend-quick.sh /usr/local/bin/globuy-compile-quick
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/compile-backend-quick.sh"
[[ -f "$TARGET" ]] || { echo "[publish-compile-quick] 找不到 $TARGET" >&2; exit 1; }
exec bash "$TARGET" "$@"
