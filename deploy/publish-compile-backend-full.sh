#!/usr/bin/env bash
# 任意目录：宿主 Gradle 全量编译（clean bootJar）
# 可选：sudo ln -sf .../publish-compile-backend-full.sh /usr/local/bin/globuy-compile-full
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/compile-backend-full.sh"
[[ -f "$TARGET" ]] || { echo "[publish-compile-full] 找不到 $TARGET" >&2; exit 1; }
exec bash "$TARGET" "$@"
