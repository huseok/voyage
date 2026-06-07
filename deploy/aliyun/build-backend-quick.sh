#!/usr/bin/env bash
# 后端快速发版：git pull（默认）→ 宿主增量 bootJar → Dockerfile.fast 打镜像 → compose up
# 日常改代码发版用这个；≤3GB 内存机脚本会自动 Gradle 768m + 构建前停 API
# 2GB 机推荐流程：globuy-stop → globuy-backend-quick →（成功后）globuy-restart
#
# 用法：
#   bash deploy/aliyun/build-backend-quick.sh
#   bash deploy/aliyun/build-backend-quick.sh --no-pull --stop-api-first
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/release-backend.sh" --backend-quick "$@"
