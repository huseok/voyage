#!/usr/bin/env bash
# 后端全量发版：git pull（默认）→ Docker 镜像无缓存重建（容器内 Gradle，standard 路径）
# 依赖/镜像缓存异常时用；比 quick 慢很多。**2GB 内存机请勿用**，极易 OOM，请用 build-backend-quick 或 compile-backend-full。
#
# 若只需宿主 clean bootJar（不重建 Docker 镜像），用 compile-backend-full.sh。
# 若需「宿主 clean bootJar + 快速镜像」组合，用：
#   bash compile-backend-full.sh && build-backend-quick.sh --no-pull
#
# 用法：
#   bash deploy/aliyun/build-backend-full.sh
#   bash deploy/aliyun/build-backend-full.sh --no-pull
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/release-backend.sh" --backend-full "$@"
