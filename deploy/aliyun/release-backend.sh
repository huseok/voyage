#!/usr/bin/env bash
# 仅发布后端：git pull（默认）→ 按 release.sh 规则构建镜像并 docker compose up
# 用法：bash deploy/aliyun/release-backend.sh [--no-pull|--backend-standard|--backend-full|--stop-api-first 等]
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/release.sh" --backend-only "$@"
