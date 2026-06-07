#!/usr/bin/env bash
# 任意目录：后端全量发版（Docker 无缓存重建）
# 可选：sudo ln -sf .../publish-backend-full.sh /usr/local/bin/globuy-backend-full
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/build-backend-full.sh"
[[ -f "$TARGET" ]] || { echo "[publish-backend-full] 找不到 $TARGET" >&2; exit 1; }
exec bash "$TARGET" "$@"
