#!/usr/bin/env bash
# 任意目录执行：默认调用 release-backend.sh（= 快速发版 build-backend-quick.sh）
# 全量发版请用 publish-backend-full.sh；仅编译 JAR 请用 publish-compile-backend-quick.sh / -full.sh
# 可选：sudo ln -sf .../publish-backend.sh /usr/local/bin/globuy-backend
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/release-backend.sh"
[[ -f "$TARGET" ]] || { echo "[publish-backend] 找不到 $TARGET（请设置 VOYAGE_REPO）" >&2; exit 1; }
exec bash "$TARGET" "$@"
