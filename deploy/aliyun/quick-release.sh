#!/usr/bin/env bash
# Globuy 快速增量发版（日常默认）
# 前端：npm run build（增量，不 clean / 不强制 npm ci）
# 后端：宿主增量 bootJar + Dockerfile.fast + docker compose up
# 最后：nginx reload
#
# 用法：
#   globuy-quick                    # 前后端都发（默认 git pull）
#   globuy-quick --backend-only     # 只发后端
#   globuy-quick --frontend-only    # 只发前端
#   globuy-quick --no-pull          # 不拉代码，用服务器当前代码
#   globuy-quick --no-pull --backend-only
#
# 小内存机（约 1.6～2GB）编译前可先：globuy-stop && sudo systemctl stop docker
# 发版成功后：sudo systemctl start docker && globuy-restart（若曾停过 docker/nginx）
set -euo pipefail

GLOBUY_ROOT="${GLOBUY_ROOT:-/opt/globuy}"
VOYAGE_REPO="${VOYAGE_REPO:-$GLOBUY_ROOT/repo/voyage}"
TARGET="$VOYAGE_REPO/deploy/aliyun/release.sh"
[[ -f "$TARGET" ]] || { echo "[quick-release] 找不到 $TARGET（请设置 VOYAGE_REPO）" >&2; exit 1; }

exec bash "$TARGET" --backend-quick "$@"
