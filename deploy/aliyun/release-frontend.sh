#!/usr/bin/env bash
# 仅发布前端：git pull（默认）→ npm ci && build → rsync 到 www/frontend
# 用法：bash deploy/aliyun/release-frontend.sh [--no-pull 等]，参数透传给 release.sh
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/release.sh" --frontend-only "$@"
