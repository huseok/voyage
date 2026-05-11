#!/usr/bin/env bash
# 任意目录执行：默认调用 $VOYAGE_REPO/deploy/aliyun/release-backend.sh
# 可选放到 /opt/globuy/bin 并 chmod +x，或：sudo ln -sf /opt/globuy/repo/voyage/deploy/publish-backend.sh /usr/local/bin/globuy-backend
set -euo pipefail
GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/release-backend.sh"
[[ -f "$TARGET" ]] || { echo "[publish-backend] 找不到 $TARGET（请设置 VOYAGE_REPO）" >&2; exit 1; }
exec bash "$TARGET" "$@"
