# 阿里云 Ubuntu 部署：分步命令说明（用于查阅 / 备忘）

面向：**Docker / docker-compose、Nginx、双仓库（voyage + foreign-trade-shop）**。变量与密钥勿提交 Git。

**日常重新发版**：在服务器上对 `release.sh` 赋可执行权限后直接执行即可（默认：前后端 `git pull`、前端 `npm ci && build` 并同步到 `/opt/globuy/www/frontend`、后端 **默认 quick**：宿主 `./gradlew bootJar` + `Dockerfile.fast` 再 `docker compose up -d --build`，需 **JDK 17+**；未装 JDK 请加 **`--backend-standard`**、最后 `nginx` reload）：

```bash
chmod +x /opt/globuy/repo/voyage/deploy/aliyun/release*.sh
bash /opt/globuy/repo/voyage/deploy/aliyun/release.sh
# 仅前端 / 仅后端（等价于 release.sh --frontend-only / --backend-only）：
# bash …/release-frontend.sh
# bash …/release-backend.sh
```

选项与路径覆盖见 **`./deploy/aliyun/release.sh --help`**（含 `--no-pull`、`--frontend-only`、`--backend-only`、`--no-nginx`、**`--stop-api-first`**、**`--backend-standard`**（无 JDK 时用）、**`--backend-quick`**（与默认相同）、**`--backend-full`**（无缓存全量构建）及 `GLOBUY_ROOT` 等）。**首次装机**仍建议按下面步骤准备目录、`env.backend` 与 Nginx 站点配置。

---

## 步骤 0：服务器上要有什么

| 内容 | 作用 |
|------|------|
| Ubuntu + SSH | 远程登录；**长时间构建建议用本机 `ssh`，少用控制台网页终端**（Workbench 易超时、常不宜多开） |
| Docker | 跑 Postgres、后端容器 |
| **`docker compose` V2 插件**（`docker-compose-plugin`，推荐） | 按 yml 一键起服务；**不要用** Ubuntu 自带的 Python **`docker-compose` 1.29** 配新版 Docker，易 **`ContainerConfig`** 报错 |
| Node.js **≥ 20** | 前端 **`npm run build`**（旧版 Node 12 会报 `Unexpected token '?'`） |
| Nginx | 对外 `:80` 静态前端 + 反代 `/api`、`/media` |

---

## 步骤 1：创建固定目录（数据与代码分开，方便找）

```bash
sudo mkdir -p /opt/globuy/{repo,data/postgres,data/media,www/frontend,config,logs}
sudo chown -R "$USER":"$USER" /opt/globuy
```

| 路径 | 干什么用 |
|------|----------|
| `/opt/globuy/repo/voyage` | 后端 Git 克隆目录 |
| `/opt/globuy/repo/foreign-trade-shop` | 前端 Git 克隆目录 |
| `/opt/globuy/data/postgres` | **数据库文件落盘**（Docker 卷挂载到这里） |
| `/opt/globuy/data/media` | **上传图片等媒体**（后端容器挂载） |
| `/opt/globuy/www/frontend` | 前端 `npm run build` 后的静态文件 |
| `/opt/globuy/config/env.backend` | 后端密钥与环境变量（手工编辑） |
| `/opt/globuy/logs` | 构建 / compose 输出（`tee`、`nohup` 等，见步骤 4） |

---

## 步骤 2：拉代码

```bash
cd /opt/globuy/repo
git clone <你的 voyage 仓库> voyage
git clone <你的 foreign-trade-shop 仓库> foreign-trade-shop
```

以后更新后端：

```bash
cd /opt/globuy/repo/voyage && git pull
```

---

## 步骤 3：准备后端环境变量文件

```bash
cp /opt/globuy/repo/voyage/deploy/aliyun/env.backend.example /opt/globuy/config/env.backend
nano /opt/globuy/config/env.backend
chmod 600 /opt/globuy/config/env.backend
```

| 变量 | 干什么 | 注意 |
|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | Spring 激活哪些配置档 | Compose + 自带 Postgres：`local,aliyun` |
| `SPRING_DATASOURCE_*` | 连哪个数据库 | 与 compose 里服务名 `db`、库名一致 |
| `POSTGRES_PASSWORD` | Postgres 容器初始化密码 | 必须与 `SPRING_DATASOURCE_PASSWORD` **相同** |
| `JWT_SECRET` | 签发登录 Token | 长随机串；服务器可用 `openssl rand -base64 48` |
| `APP_CORS_ALLOWED_ORIGINS` | 浏览器跨域白名单 | 填用户**真实访问前端的地址**，如 `http://公网IP`，逗号分隔无空格 |
| `VOYAGE_MEDIA_STORAGE_ROOT` | 容器内媒体根路径 | 与 compose 挂载一致，一般为 `/data/media` |

---

## 步骤 4：构建并启动后端 + 数据库

### 长时间构建 / 启动：日志、进度、可「挂住」会话的方式

Workbench / Web SSH 容易超时断开；**Gradle 首次编译也可能十几分钟**。仓库里的 `Dockerfile` 已在 `./gradlew` 上加 **`--console=plain`**，镜像构建阶段会持续打出 Gradle 任务行。

若你用的是 **阿里云控制台网页终端**：控制台自带的 **分屏/新开终端** 常会 **建立远程连接失败**，不要用；优先下面 **单面板 `tee`** 或 **`nohup`**（见常见问题 **「Workbench 限制」**）。**本机 `ssh`** 再执行 **tmux** 最省心。

下面任选其一（推荐先把 **`logs` 目录建好**：步骤 1 已含 `logs`，或单独 `mkdir -p /opt/globuy/logs`）。

---

**方式 A — tmux + 后台容器 + 日志落盘（推荐）**

适合：**断线也不断构建**、事后 `tail -f` 看进度。

```bash
sudo apt-get update && sudo apt-get install -y tmux   # 一次性
sudo mkdir -p /opt/globuy/logs && sudo chown "$USER:$USER" /opt/globuy/logs

tmux new -s globuy
cd /opt/globuy/repo/voyage
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend up -d --build 2>&1 \
  | tee /opt/globuy/logs/docker-compose-build.log
```

- **分离会话**：`Ctrl+b` 再按 `d`（命令跑完后容器已在后台）
- **回到会话**：`tmux attach -t globuy`
- **另一终端只看进度**：`tail -f /opt/globuy/logs/docker-compose-build.log`

---

**方式 B — tmux 里「前台」跟日志（不加 `-d`，挂起到会话里）**

适合：**想一直盯着** Docker/应用输出；必须在 **tmux/screen** 里跑，否则 SSH 一断进程可能被干掉。

```bash
tmux new -s globuy
cd /opt/globuy/repo/voyage
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend up --build 2>&1 | tee /opt/globuy/logs/compose-up-foreground.log
```

注意：**不要加 `-d`**。构建并启动后日志会持续前台输出；按 **`Ctrl+C` 会停止容器**。确认跑稳后，可改用具 **`up -d`** 或先 `Ctrl+C` 再执行下面的 **`up -d`** 重新后台启动。

---

**方式 C — 先构建镜像、再起服务（分两阶段看进度）**

```bash
cd /opt/globuy/repo/voyage
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend build 2>&1 | tee /opt/globuy/logs/docker-compose-build-only.log

DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend up -d
```

---

**方式 D — 不用 tmux：`nohup` 后台写日志**

```bash
cd /opt/globuy/repo/voyage
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
nohup env DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend up -d --build \
  > /opt/globuy/logs/docker-compose-build.log 2>&1 &
tail -f /opt/globuy/logs/docker-compose-build.log
```

---

**运行起来之后跟应用日志**

```bash
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend logs -f voyage-api
```

---

进入**后端仓库根目录**：

```bash
cd /opt/globuy/repo/voyage
```

若之前开过 BuildKit 导致 **buildx 报错**，先关掉再构建：

```bash
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
```

使用 **经典 docker-compose（带横杠）** 时：

```bash
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend up -d --build
```

| 部分 | 干什么 |
|------|--------|
| `-f deploy/aliyun/docker-compose.stack.yml` | 指定栈文件（Postgres + voyage-api） |
| `--env-file /opt/globuy/config/env.backend` | 把密钥注入 compose |
| `up -d` | 后台启动容器 |
| `--build` | 本地构建后端镜像（`Dockerfile` 内 `./gradlew ... --console=plain`，**首次可能 15～30 分钟**；本机 SSH 可用 **tmux + tee**；Workbench 单通道请用 **单面板 tee / nohup**，勿开控制台分屏 |
| `DOCKER_BUILDKIT=0` | 避免未装 **docker-buildx** 的机器构建失败 |

看后端日志：

```bash
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend logs -f voyage-api
```

本机测 API（API 只监听 `127.0.0.1:8080`，外网直连端口不通是正常的）：

```bash
curl -sS http://127.0.0.1:8080/api/v1/tags | head
```

---

## 步骤 5：构建前端并放到 Nginx 目录

需要 **Node.js ≥ 20**（`node -v`）。若为 **v12/v14**，`npm run build` 可能在 TypeScript 处报 **`SyntaxError: Unexpected token '?'`**——请先升级 Node（见下文「Ubuntu 上安装 Node 20」）。

### Ubuntu 上安装 Node 20（示例：NodeSource）

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
node -v   # 应显示 v20.x
```

---

```bash
cd /opt/globuy/repo/foreign-trade-shop
cp .env.production.example .env.production
# 与 Nginx 同域反代 /api 时：保持 VITE_API_BASE_URL 为空
npm ci
# 勿用 npm ci --omit=dev：`npm run build` 会先跑 codegen（需 devDependency openapi-typescript）
npm run build
rsync -a --delete dist/ /opt/globuy/www/frontend/
```

| 命令 | 干什么 |
|------|--------|
| `npm run build` | 先根据 `openapi/openapi.json` **生成** `src/generated/voyage-paths.ts`，再 `tsc` + `vite build` 产出 `dist/` |
| `rsync ... /opt/globuy/www/frontend/` | 与 Nginx `root` 对齐 |

---

## 步骤 6：Nginx

```bash
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.example /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

| 配置意图 | 说明 |
|----------|------|
| `root /opt/globuy/www/frontend` | 前端 SPA |
| `location /api/` → `127.0.0.1:8080` | 反向代理后端 |
| `location /media/` → `127.0.0.1:8080` | 商品图等 |

云安全组：**放行 80**；**不要**对 `0.0.0.0/0` 放行 **5432、8080**（本方案 DB/API 只本机访问）。

---

## 常见问题速查

| 现象 | 处理 |
|------|------|
| `unknown shorthand flag: 'f'` | 可能用了错误的 compose 子命令；应使用 **`docker compose`**（V2 插件，中间有空格）或 **`docker-compose`**（仅在没有插件时的旧命令） |
| `Recreating globuy-api … KeyError: 'ContainerConfig'`、`docker-compose==1.29.x` | **Compose V1（Python）与新版 Docker Engine 不兼容**。安装 V2：`sudo apt-get update && sudo apt-get install -y docker-compose-plugin`，用 **`docker compose`**（勿用旧 `docker-compose`）。删掉失败容器后重建：`docker rm -f globuy-api`，再在 voyage 根目录执行 **`bash deploy/aliyun/release-backend.sh`** |
| 浏览器 **`502 Bad Gateway`**、`/api/` 全挂 | 多为 **后端容器未起来**（参见上一行）或 Nginx **`proxy_pass`** 端口不对；服务器上执行 **`docker ps`** 看是否有 **`globuy-api`**，`curl -sS http://127.0.0.1:8080/api/v1/tags` 能否通 |
| BuildKit / buildx 报错 | **`unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD`** 后 **`DOCKER_BUILDKIT=0`** 再 **`docker compose`** / **`docker-compose`** … `build` |
| Gradle 停在 `Daemon will be stopped...` 很久 | 见下文 **「Gradle 看似卡住」**；该行多半出现在**构建开头**，后面可能在静默拉依赖/编译 |
| Workbench **第二个连接**失败：`SocketTimeoutException`、**建立远程连接失败**、`ecs-workbench-inner-share...` | 见下文 **「Workbench 限制」**；控制台 **分屏/新终端** 往往等于第二条通道 |
| CORS 报错 | 检查 `APP_CORS_ALLOWED_ORIGINS` 是否包含浏览器地址栏里的 **完整 Origin**（含 `http://` 与端口） |

### Workbench 网页 SSH 的限制（含「同一页分屏」）

阿里云控制台里的 **网页远程** 经常出现：**只允许一条活跃的远程通道**。下列操作常被算作「再连一次」，从而 **`SocketTimeoutException`** 或 **`建立远程连接失败`**：

- 再开一个 **Workbench 标签页 / 新网页终端**
- 控制台界面提供的 **分屏再开一个终端**（同样是第二条连接，**不是** Linux 里 tmux 那种只占一个 SSH 会话的分屏）

这不是你命令写错，而是 **Workbench 网关/产品策略** 限制。

**可行做法（按优先级）：**

1. **务必改用本机 SSH（强烈推荐）**  
   在你自己的电脑上：`ssh ubuntu@<公网IP>`（用户名按镜像：`root` / `ubuntu` / `ecs-user`）。  
   **安全组入方向放行 TCP 22**，来源可先写成你家宽带公网 IP。  
   本机可以多开窗口、`tmux`、`tail -f`，不再受 Workbench 单通道限制。

2. **只能固守一个 Workbench 面板时（不要分屏、不要新开终端）**  
   **整段只用这一条前台命令**，输出既上屏又落盘（**只占一条连接**）：

```bash
cd /opt/globuy/repo/voyage
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend up -d --build 2>&1 \
  | tee /opt/globuy/logs/docker-compose-build.log
```

   或：`nohup ... > /opt/globuy/logs/docker-compose-build.log 2>&1 &` 后，**同一面板**里执行 `tail -f /opt/globuy/logs/docker-compose-build.log`（先后台再跟日志，**不要**再点控制台分屏）。

3. **若本机暂时不能 SSH（例如未放行 22）**  
   在控制台 **网络与安全 → 安全组** 给 ECS 绑定的安全组加一条：**入方向 TCP 22**，来源填你当前公网 IP；再用本机 `ssh`。Workbench 只适合短时敲几条命令，**不适合长时间构建的唯一入口**。

---

### Gradle 看似卡住（`Daemon will be stopped at the end of the build` 之后很久没新输出）

1. **先别急着停**：`--no-daemon` 时这句话常在**任务刚开始**就打出来；后面可能是 **Maven Central 等依赖下载**、**Kotlin 编译**，在 **1 vCPU / 2GB** 小机上首次 **30～60 分钟** 都见过，不一定真死锁。
2. **判断是否还在干活**：  
   - **本机 SSH**：另开窗口执行 `docker stats` 或 `top`，看 **`java`** / 构建容器 CPU 是否长期 **>0**。  
   - **只剩 Workbench 单面板**：不要开控制台分屏；可看 **`tail -f`** 日志是否持续增长，或隔几分钟在同一面板执行 **`wc -c /opt/globuy/logs/docker-compose-build.log`** 看文件是否在变大。
3. **若 CPU 长时间接近 0、内存也不动**：更像 **网络卡住**（连 Maven 很慢或被拦）。可换机器网络/安全组；或在能顺畅访问 Maven 的环境 **本机 `bootJar`** 后用仓库里的 **`Dockerfile.fast`**，只把 `build/libs/*.jar` 拷进镜像（几秒级）。
4. **下次构建想多看日志**：可把 `Dockerfile` 里那一行改成带 **`--info`**（日志会变多、稍慢一点）：  
   `./gradlew bootJar -x test --no-daemon --stacktrace --console=plain --info`

---

## 代码里与部署相关的配置（不写死，用环境变量）

| 用途 | 位置 |
|------|------|
| 数据库 JDBC | `SPRING_DATASOURCE_URL` 等 + `application-cloud.yaml` / `application-local.yaml` |
| 媒体目录 | `VOYAGE_MEDIA_STORAGE_ROOT` + `application.yaml` → `voyage.media.storage-root` |
| JWT / CORS | `JWT_SECRET`、`APP_CORS_ALLOWED_ORIGINS` |
| 生产日志 profile | `SPRING_PROFILES_ACTIVE` 带 **`aliyun`** → `application-aliyun.yaml` |
| 前端 API 根地址 | `foreign-trade-shop` 的 **`VITE_API_BASE_URL`**（同域可留空） |

---

## 停止 / 重启栈（备忘）

在后端仓库目录：

```bash
cd /opt/globuy/repo/voyage
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend down
```

仅重启 API（示例）：

```bash
DOCKER_BUILDKIT=0 docker-compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend restart voyage-api
```

**删除数据库卷（慎重）**：`down -v` 会删掉 compose 管理的命名卷；本方案 Postgres 数据在 **bind mount** `/opt/globuy/data/postgres`，删容器不等于删该目录，需手工 `rm` 才清空。
