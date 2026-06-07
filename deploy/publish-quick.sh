#!/usr/bin/env bash
# 任意目录：快速增量发版（前后端或 --backend-only / --frontend-only）
# 推荐：sudo ln -sf /opt/globuy/repo/voyage/deploy/publish-quick.sh /usr/local/bin/globuy-quick
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/quick-release.sh"
[[ -f "$TARGET" ]] || { echo "[publish-quick] 找不到 $TARGET（请设置 VOYAGE_REPO）" >&2; exit 1; }
exec bash "$TARGET" "$@"
