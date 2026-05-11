# 阿里云 Ubuntu 单机部署（文档在后端仓库 `voyage/deploy/aliyun`）

**日常一键发版（可选）：** 请用 **`bash`** 执行（勿用 **`sh`**，Ubuntu 上常为 dash）。**`deploy/aliyun/release.sh`**：全量；**`release-frontend.sh`** / **`release-backend.sh`**：只前端 / 只后端。不想每次 **`cd deploy/aliyun`** 或敲长路径时，可用 **`deploy/publish-frontend.sh`**、**`deploy/publish-backend.sh`**（任意目录执行，默认 **`VOYAGE_REPO=/opt/globuy/repo/voyage`**）；进一步改成短命令见下文 **「快捷命令」**。其余参数透传（如 **`--no-pull`**）。需要减轻构建期争抢可加 **`--stop-api-first`**。**后端默认「快速」**：宿主 **`./gradlew bootJar`** + **`Dockerfile.fast`**（需 **JDK 17+**）；未装 JDK 用 **`--backend-standard`** 或 **`BACKEND_BUILD_MODE=standard`**；依赖或镜像缓存可疑用 **`--backend-full`**。详见 **`bash release.sh --help`** 与 **[`DEPLOY_STEP_BY_STEP.md`](./DEPLOY_STEP_BY_STEP.md)**。

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

## 快捷命令（可选，仅服务器配置）

脚本已支持任意目录执行（`publish-*.sh` 会解析 `VOYAGE_REPO`）。不想每次敲 **`/opt/globuy/repo/voyage/...`** 全路径时，在**服务器上**任选一种即可（**无需改仓库里的脚本**）。

### 方式 A：`/usr/local/bin` 软链接（推荐，需 sudo）

```bash
sudo ln -sf /opt/globuy/repo/voyage/deploy/publish-frontend.sh /usr/local/bin/globuy-frontend
sudo ln -sf /opt/globuy/repo/voyage/deploy/publish-backend.sh  /usr/local/bin/globuy-backend
sudo ln -sf /opt/globuy/repo/voyage/deploy/aliyun/release.sh    /usr/local/bin/globuy-release
sudo chmod +x /opt/globuy/repo/voyage/deploy/publish-frontend.sh \
              /opt/globuy/repo/voyage/deploy/publish-backend.sh \
              /opt/globuy/repo/voyage/deploy/aliyun/release.sh \
              /opt/globuy/repo/voyage/deploy/aliyun/release-frontend.sh \
              /opt/globuy/repo/voyage/deploy/aliyun/release-backend.sh
```

之后示例：`globuy-release`、`globuy-frontend --no-pull`、`globuy-backend --backend-standard`。若代码不在默认路径，调用前设置 **`export GLOBUY_ROOT=…`**（或 **`VOYAGE_REPO`**）即可。

### 方式 B：`~/bin` + `PATH`（无需 sudo）

```bash
mkdir -p ~/bin
ln -sf /opt/globuy/repo/voyage/deploy/publish-frontend.sh ~/bin/globuy-frontend
ln -sf /opt/globuy/repo/voyage/deploy/publish-backend.sh  ~/bin/globuy-backend
ln -sf /opt/globuy/repo/voyage/deploy/aliyun/release.sh    ~/bin/globuy-release
chmod +x ~/bin/globuy-*
grep -q 'export PATH="$HOME/bin:$PATH"' ~/.bashrc || echo 'export PATH="$HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

### 方式 C：shell 别名（个人账号）

在 **`~/.bashrc`** 末尾追加：

```bash
alias gb-front='bash /opt/globuy/repo/voyage/deploy/publish-frontend.sh'
alias gb-back='bash /opt/globuy/repo/voyage/deploy/publish-backend.sh'
alias gb-all='bash /opt/globuy/repo/voyage/deploy/aliyun/release.sh'
```

执行 **`source ~/.bashrc`** 后可用 **`gb-all`** / **`gb-front`** / **`gb-back`**。

## 后端 Docker

```bash
cd /opt/globuy/repo/voyage
cp deploy/aliyun/env.backend.example /opt/globuy/config/env.backend
nano /opt/globuy/config/env.backend   # 密码、JWT、CORS（含 http://公网IP）
chmod 600 /opt/globuy/config/env.backend
# 须使用 Compose V2。若 apt 提示找不到 docker-compose-plugin、且 docker compose 报错 unknown command（阿里云常见），见 DEPLOY_STEP_BY_STEP.md「安装 Compose V2」。也可用 GitHub 二进制 docker-compose 2.x 到 /usr/local/bin。
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
export DOCKER_BUILDKIT=0
docker compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend up -d --build
```

验证：`curl -sS http://127.0.0.1:8080/api/v1/tags | head`

## 前端静态资源

```bash
cd /opt/globuy/repo/foreign-trade-shop
cp .env.production.example .env.production   # 同域反代时 VITE_API_BASE_URL 留空
npm ci && npm run build
# 一键脚本默认先试 build，失败再询问是否 npm ci；强制每次 ci：--frontend-ci-first；全量删目录：--frontend-clean；VERBOSE：RELEASE_VERBOSE=1
# 小机防卡：release.sh 默认 RELEASE_FRONTEND_FAST_BUILD=1（跳过 React Compiler，构建快）；要 Compiler：RELEASE_FRONTEND_FAST_BUILD=0，或在本机/CI 执行 npm run build 后只 rsync dist
# 前端 vite：默认 React Compiler 只处理 src 下除 admin 以外的页面（缩短构建）；全量含后台：REACT_COMPILER_ALL_SRC=1 npm run build
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
