#!/usr/bin/env bash
# 任意目录执行：调用 $VOYAGE_REPO/deploy/aliyun/release.sh（避免 globuy-release 直链 release.sh 时找不到 _common.sh）
# 推荐：sudo ln -sf .../publish-release.sh /usr/local/bin/globuy-release
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/release.sh"
[[ -f "$TARGET" ]] || { echo "[publish-release] 找不到 $TARGET（请设置 VOYAGE_REPO）" >&2; exit 1; }
exec bash "$TARGET" "$@"
