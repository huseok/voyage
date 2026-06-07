#!/usr/bin/env bash
# 任意目录：后端快速发版（宿主 bootJar + Dockerfile.fast）
# 可选：sudo ln -sf .../publish-backend-quick.sh /usr/local/bin/globuy-backend-quick
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/build-backend-quick.sh"
[[ -f "$TARGET" ]] || { echo "[publish-backend-quick] 找不到 $TARGET" >&2; exit 1; }
exec bash "$TARGET" "$@"
