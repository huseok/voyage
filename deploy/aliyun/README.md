# 阿里云 Ubuntu 单机部署（文档在后端仓库 `voyage/deploy/aliyun`）

**日常一键发版（可选）：** 服务器上执行 **`release.sh`**（默认 pull + 前端 build + Docker + nginx reload）。需要减轻构建期争抢时可加 **`--stop-api-first`**。**后端默认「快速」**：在服务器执行 **`./gradlew bootJar`**，再用 **`Dockerfile.fast`** 打镜像（需 **JDK 17+**）。若机器未装 JDK，请加 **`--backend-standard`** 或设置 **`BACKEND_BUILD_MODE=standard`**（改回容器内 Gradle，较慢）。依赖或镜像缓存可疑时用 **`--backend-full`**（`docker build --no-cache`）。详见 **`release.sh --help`** 与 **[`DEPLOY_STEP_BY_STEP.md`](./DEPLOY_STEP_BY_STEP.md)**。

**分步命令说明（推荐打印 / 收藏）：** [`DEPLOY_STEP_BY_STEP.md`](./DEPLOY_STEP_BY_STEP.md)

假设：**后端**与**前端**是两个独立 Git 仓库；服务器上放在同一父目录下即可。

## 目录约定

| 路径 | 用途 |
|------|------|
| `/opt/globuy/repo/voyage` | 后端仓库（含本 `deploy/aliyun`） |
| `/opt/globuy/repo/foreign-trade-shop` | 前端仓库（名称可自定） |
| `/opt/globuy/data/postgres`、`data/media` | 数据库与上传文件 |
| `/opt/globuy/www/frontend` | 前端构建产物 |
| `/opt/globuy/config/env.backend` | 后端环境变量（勿提交） |

## 后端 Docker

```bash
cd /opt/globuy/repo/voyage
cp deploy/aliyun/env.backend.example /opt/globuy/config/env.backend
nano /opt/globuy/config/env.backend   # 密码、JWT、CORS（含 http://公网IP）
chmod 600 /opt/globuy/config/env.backend
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
docker-compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend up -d --build
```
# DOCKER_BUILDKIT=0  强制关闭新版，用老版传统构建方式



验证：`curl -sS http://127.0.0.1:8080/api/v1/tags | head`

## 前端静态资源

```bash
cd /opt/globuy/repo/foreign-trade-shop
cp .env.production.example .env.production   # 同域反代时 VITE_API_BASE_URL 留空
npm ci && npm run build
rsync -a --delete dist/ /opt/globuy/www/frontend/
```

## Nginx

```bash
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.example /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

安全组放行 **80**；勿对公网开放 **5432**、**8080**。

配置项说明见后端 `application.yaml`、`application-cloud.yaml`、`application-aliyun.yaml` 及前端 `.env.production.example`。
