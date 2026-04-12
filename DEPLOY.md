# Voyage：Docker 运行后端

默认：**容器内 Spring** + **远端 PostgreSQL**（`SPRING_PROFILES_ACTIVE` 默认 **`cloud`**，见 **`application-cloud.yaml`**）。

**可选**：**本机 Postgres 跑在 Docker**（`--profile local-db` + 专用 env 文件），见 **§3.1**。本机 **`gradlew bootRun`** 见 **§4**。

---

## 1. 配置文件职责

| 文件 | 说明 |
|------|------|
| **`application-cloud.yaml`** | **profile=cloud**：远端库默认 JDBC / 用户名等。 |
| **`application-local.yaml`** | **profile=local**：默认连 **`localhost:5432`** 的 `voyage_db`（本机只起 `db` 容器 + Gradle 时用）。 |
| **`docker-compose.yml`** | 默认只起 API；加 **`--profile local-db`** 时起内置 Postgres；变量用 **`--env-file`** 或 `.env` 注入（见 §3）。 |

---

## 2. 容器内 Spring 至少需要的环境变量

| 变量 | 说明 |
|------|------|
| **`SPRING_PROFILES_ACTIVE`** | 默认 **`cloud`**；本地 Docker 库时设为 **`local`**（与 **`application-local.yaml`** 一致）。 |
| **`SPRING_DATASOURCE_PASSWORD`** | Compose 占位符 **`${SPRING_DATASOURCE_PASSWORD}`**（与 **`.env.render.local` / Render** 同名）。**必须非空**，否则应用起不来。 |
| **`SPRING_DATASOURCE_URL`** / **`USERNAME`** | 可选；compose 里带有默认值，可用 shell / `.env` 整串覆盖。 |
| **`JWT_SECRET`** | 建议生产改为长随机串；compose 带默认值。 |
| **`JWT_ACCESS_TOKEN_MINUTES`**（可选） | Access JWT 有效期，默认 **15** 分钟；到期前前端会静默 refresh。 |
| **`JWT_REFRESH_TOKEN_DAYS`**（可选） | Refresh JWT 最长有效天数，默认 **14**；到期后须重新登录。 |
| **`APP_CORS_ALLOWED_ORIGINS`** | 浏览器 **Origin** 白名单，**英文逗号分隔、不要空格**。本机 Vite 常见：`http://localhost:5173`（若只用 `.env.render.local` 配了线上 Vercel，**务必把 localhost 一并写上**，否则本地登录会 CORS 预检失败）。未设时 compose 默认带 5173/5174。 |

密码未进容器时，日志里常见：  
`The server requested SCRAM-based authentication, but no password was provided.`

---

## 3. Docker Compose：正确启动命令

**要点**：`docker-compose.yml` 里的 **`${SPRING_DATASOURCE_PASSWORD}`**、**`${JWT_SECRET}`** 等，由 Compose 在解析文件时替换，来源优先级为：**当前 shell 环境变量** → **`--env-file`** → **同目录 `.env`**（若存在）。

### 3.1 本地 Postgres（Docker 里的 `db` + API 同网）

1. 在 **`voyage` 根目录** 新建 **`.env.docker.local`**（可提交到 Git；若含与生产不同的密码，仍建议勿把生产密钥写进此文件）。内容示例（**密码与 `POSTGRES_PASSWORD` 必须一致**）：

```env
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/voyage_db
SPRING_DATASOURCE_USERNAME=voyage_user
SPRING_DATASOURCE_PASSWORD=change_me
POSTGRES_PASSWORD=change_me
JWT_SECRET=change_me_to_a_long_secret_for_prod
```

2. 启动（**必须**带 **`--profile local-db`** 与同一 **`--env-file`**）：

```powershell
cd <本仓库>\voyage
docker compose --profile local-db --env-file .env.docker.local up -d --build
```

3. 停止并删掉 compose 内 Postgres 数据卷（慎用）：

```powershell
docker compose --profile local-db --env-file .env.docker.local down -v
```

不设 **`--profile local-db`** 时不会起 **`db`** 容器，行为与原先「仅远端库」一致。

**只起 `db`、应用在宿主机用 Gradle**：先 `docker compose --profile local-db --env-file .env.docker.local up -d db`，再在终端设 **`SPRING_PROFILES_ACTIVE=local`**（数据源走 **`application-local.yaml`** 默认的 **`localhost:5432`**；库用户密码与 **`POSTGRES_PASSWORD` / `SPRING_DATASOURCE_PASSWORD`** 与 `.env.docker.local` 里一致即可）。

### 方式 A：使用 `.env.render.local`（远端库，与 Render 清单一致）

在 **`voyage` 根目录** 执行（**每一行**都带上 **`--env-file .env.render.local`**，Compose 才会用你的文件做 **`${...}` 替换**）：

```powershell
cd <本仓库>\voyage
docker compose --env-file .env.render.local up -d --build
```

查看日志、确认已启动后再访问 Swagger：

```powershell
docker compose --env-file .env.render.local logs -f voyage-api
```

停止：

```powershell
docker compose --env-file .env.render.local down
```

你的 **`.env.render.local`** 里已有 **`SPRING_DATASOURCE_PASSWORD`** 等时，会覆盖 compose 默认值；**`SPRING_PROFILES_ACTIVE`** 可不写（默认 **`cloud`**）或显式写 **`cloud`**。

### 方式 A2：使用默认 `.env`

把变量拷到 **`voyage/.env`**（文件名必须是 `.env`），然后：

```powershell
cd <本仓库>\voyage
docker compose up -d --build
```

示例：

```env
SPRING_DATASOURCE_PASSWORD=你的远端数据库密码
JWT_SECRET=足够长的随机串
# 可选：SPRING_DATASOURCE_URL、API_PORT 等
```

### 看起来像「卡住」：多半是镜像在编译（正常）

`--build` 会在镜像里跑 **Gradle `bootJar`**（拉依赖 + 编译 Kotlin）。**第一次或清缓存后常见 5～20 分钟**，终端若只停在某一行（例如 `Daemon will be stopped...`）**不等于死机**，Gradle 仍在跑。

建议：

1. **看完整构建日志**（有进度输出）：

   ```powershell
   $env:DOCKER_BUILDKIT = "1"
   docker compose --env-file .env.render.local build --progress=plain voyage-api
   docker compose --env-file .env.render.local up -d
   ```

2. **第二次起** 会快很多：`Dockerfile` 已用 **BuildKit 缓存 `~/.gradle`**，且与 **`gradle:8.14.4-jdk17` + 本仓库 `gradlew` 版本** 对齐。

### 想快：本机打 JAR，再只做「复制进镜像」（推荐日常改代码）

在 **`voyage` 根目录**：

```powershell
.\gradlew.bat bootJar -x test
$env:DOCKERFILE = "Dockerfile.fast"
docker compose --env-file .env.render.local up -d --build
$env:DOCKERFILE = $null   # 用完可清掉，避免下次误用
```

前提：存在 **`build/libs/*.jar`**（上一步会生成）。此路径下 Docker 构建通常 **几十秒内** 结束。

### 方式 B：不建 `.env`，用当前终端注入（适合 CI / 临时）

**PowerShell：**

```powershell
cd <本仓库>\voyage
$env:SPRING_DATASOURCE_PASSWORD = "你的远端数据库密码"
docker compose up -d --build
```

**cmd：**

```cmd
cd /d <本仓库>\voyage
set SPRING_DATASOURCE_PASSWORD=你的远端数据库密码
docker compose up -d --build
```

### 起不来 / 浏览器打不开时

1. 看容器是否在跑：`docker compose ps`  
2. 看启动报错：`docker compose logs --tail 100 voyage-api`  
3. 确认本机访问端口与映射一致：默认 **`http://localhost:8080/swagger-ui/index.html`**；若设置了 **`API_PORT`**，用 **`http://localhost:<API_PORT>/swagger-ui/index.html`**。

构建阶段日志里出现 **`gradle bootJar`** 属于**镜像构建**，要等整段 build 结束、容器 `Started` 后再访问；首次构建可能较久。

### 构建失败：`gradle.properties` / `COPY ... not found`

仓库若没有 **`gradle.properties`**，旧版 `Dockerfile` 会在 `COPY` 时报错。请拉取最新 **`Dockerfile`**（已去掉该文件），或在本机补一个空的 `gradle.properties`。

### 运行期：`Network is unreachable` 连不上 Supabase

在 **Docker 里用直连主机 `db.<项目>.supabase.co`** 时，常解析到 **IPv6** 或网络路径不通，日志里会出现 **`java.net.SocketException: Network is unreachable`**。

处理任选其一：

- **推荐**：在 **`.env.render.local`** 里显式写上 Supabase 控制台提供的 **Pooler（Session）** JDBC 与用户名（与 **`application-cloud.yaml`** 默认一致时可不写，compose 已用 pooler 作默认值）；或  
- 仅在宿主机用直连，容器内一律用 **pooler** 连接串。

清理旧 compose 遗留容器（例如已删除的 `db` 服务）：

```powershell
docker compose --env-file .env.render.local down --remove-orphans
```

---

## 4. 可选：本机 Gradle（`bootRun`）

**`gradlew bootRun` 不会自动读取** 仓库里的 `.env` / `.env.render.local`。请用 **IDE 的 EnvFile / Run Configuration**，或先在终端里把变量写进当前进程再启动。

- **连远端库**：`SPRING_PROFILES_ACTIVE=cloud` + 云库密码等（与 **§3 方式 A** 相同变量名）。  
- **连本机 Docker 里的 Postgres**：先按 **§3.1** 只起 **`db`**，再 **`SPRING_PROFILES_ACTIVE=local`**（默认 **`localhost:5432/voyage_db`**）。

**PowerShell 示例**（从 `.env.render.local` 或 `.env` 读入 `KEY=VALUE` 行）：

```powershell
cd <本仓库>\voyage
$envFile = ".env.render.local"   # 或 ".env"
Get-Content $envFile | ForEach-Object {
  $t = $_.Trim()
  if ($t -match '^#' -or $t -eq '') { return }
  $i = $t.IndexOf('=')
  if ($i -lt 1) { return }
  Set-Item -Path ("env:" + $t.Substring(0, $i).Trim()) -Value $t.Substring($i + 1).Trim()
}
.\gradlew.bat bootRun
```

---

## 5. 部署到云主机 / PaaS

与 Docker 相同：在平台里配置 **与 §2 同名** 的环境变量；不必把含密码的 `.env` 提交到 Git。  
**若使用 ClawCloud Run 等「按镜像部署」的平台，请直接看 §6：准备镜像、推送仓库、在控制台创建应用。**

---

## 6. 准备镜像与发布（Docker Hub → ClawCloud Run 等）

本节说明：**在 `voyage` 根目录用 Dockerfile 构建镜像 → 推到可公网拉取的镜像仓库 → 在 PaaS 填镜像名与环境变量**。以下命令以 **PowerShell** 为例；**cmd** 可把 `cd` 换成 `cd /d`。

### 6.1 前置条件

- 已安装 **Docker Desktop**（或等价 Docker 引擎），且 **`docker version`** 客户端与服务端均正常。
- 已注册 **Docker Hub**（或 **GHCR** 等）账号；**镜像名中的命名空间必须是你在该平台上拥有推送权限的名字**（例如 `zhangsan/voyage-api`，不能随意占用他人命名空间）。

### 6.2 本地构建镜像（标准 Dockerfile）

在 **`voyage` 根目录**（与 **`Dockerfile`**、**`gradlew`** 同级）执行：

```powershell
cd <本仓库>\voyage
docker build -t <你的DockerHub用户名>/voyage-api:<版本标签> .
```

示例（仅说明格式，请替换用户名与标签）：

```powershell
cd e:\workspace\globuy\voyage
docker build -t zhangsan/voyage-api:1.0.0 .
```

- **首次构建**或清缓存后，镜像内会执行 **`./gradlew bootJar`**，常见 **5～20 分钟**；再次构建会命中 BuildKit / Gradle 缓存，通常明显变快。
- 构建成功后，可查看本地镜像：

```powershell
docker images <你的DockerHub用户名>/voyage-api --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.ID}}"
```

### 6.3 加快构建：本机先打 JAR，再用 Dockerfile.fast

与 **§3「想快」** 相同思路：先在宿主机生成 **`build/libs/*.jar`**，再用 **`Dockerfile.fast`** 只做复制，适合频繁发版。

```powershell
cd <本仓库>\voyage
.\gradlew.bat bootJar -x test
docker build -f Dockerfile.fast -t <你的DockerHub用户名>/voyage-api:<版本标签> .
```

### 6.4 登录镜像仓库并推送

推送前需 **`docker login`**（Docker Hub 默认 `docker.io`）：

```powershell
docker login
docker push <你的DockerHub用户名>/voyage-api:<版本标签>
```

推送完成后，在 **ClawCloud Run**（或其它平台）的「镜像」一栏填写完整镜像引用，例如：

- `docker.io/<你的DockerHub用户名>/voyage-api:1.0.0`（以平台提示为准，有的只填 `<用户名>/voyage-api:1.0.0`）。

**勿将**含仓库密码的脚本或 **`.env`** 提交到 Git；CI 里用 **Secret** 注入 `DOCKER_PASSWORD` 等。

### 6.5 在 ClawCloud Run 上创建应用（概要）

官方文档：**[Deploy from Docker](https://docs.run.claw.cloud/clawcloud-run/getting-started/deploy-from-docker)**。控制台入口：**[ClawCloud Run](https://run.claw.cloud/)** → **App Launchpad** → **Create App**。

建议配置要点：

| 项 | 建议值 |
|----|--------|
| 镜像 | 上一步 **`docker push`** 后的镜像名与标签 |
| 容器端口 | **8080**（与 `Dockerfile` / Spring 默认一致） |
| 公网访问 | 开启，否则浏览器无法访问 API |
| 环境变量 | 与 **§2** 一致：`SPRING_PROFILES_ACTIVE=cloud`（连远端库）、`SPRING_DATASOURCE_*`、`JWT_SECRET`、**`APP_CORS_ALLOWED_ORIGINS`**（必须包含**线上前端**完整 Origin，英文逗号分隔；若仍本地联调可追加 `http://localhost:5173`） |

数据库仍使用 **外部 PostgreSQL**（如 Supabase Pooler）；镜像内默认不自带生产库。应用启动时会执行 **Flyway 迁移**。

### 6.6 与前端联调

将前端 **`VITE_API_BASE_URL`**（或项目中等价变量）设为 Claw 分配给你的 **API 根地址**（`https://...`，是否带尾斜杠按前端 `apiClient` 约定）。

### 6.7 仅本地自测镜像（不推送）

```powershell
cd <本仓库>\voyage
docker build -t voyage-api:local .
```

运行仍需 **§2** 中的环境变量与可连上的数据库；可直接用 **§3** 的 `docker compose` 带 `--env-file`，或 `docker run -e SPRING_DATASOURCE_PASSWORD=... -e ... -p 8080:8080 voyage-api:local`（自行补全变量，勿在聊天中泄露真实密码）。
