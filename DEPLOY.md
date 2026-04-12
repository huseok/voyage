# Voyage：远端数据库 + Docker

主路径：**容器内 Spring** + **远端 PostgreSQL**。默认 profile 与数据源占位见 **`application.yaml`**、**`application-cloud.yaml`**。

**可选**：本机 **`gradlew bootRun`**，见 **§5**。

---

## 1. 配置文件职责

| 文件 | 说明 |
|------|------|
| **`application-cloud.yaml`** | 默认 JDBC / 用户名等；密码等仍由容器环境变量覆盖。 |
| **`docker-compose.yml`** | 在 `environment` 里写 Spring 所需项；`${变量名}` 由 Compose 在启动时替换（见 §3：默认 `.env` 或 **`--env-file .env.render.local`**）。 |

---

## 2. 容器内 Spring 至少需要的环境变量

| 变量 | 说明 |
|------|------|
| **`SPRING_PROFILES_ACTIVE`** | 固定 **`cloud`**（已在 compose 里写死）。 |
| **`SPRING_DATASOURCE_PASSWORD`** | Compose 占位符 **`${SPRING_DATASOURCE_PASSWORD}`**（与 **`.env.render.local` / Render** 同名）。**必须非空**，否则应用起不来。 |
| **`SPRING_DATASOURCE_URL`** / **`USERNAME`** | 可选；compose 里带有默认值，可用 shell / `.env` 整串覆盖。 |
| **`JWT_SECRET`** | 建议生产改为长随机串；compose 带默认值。 |
| **`APP_CORS_ALLOWED_ORIGINS`** | 浏览器 **Origin** 白名单，**英文逗号分隔、不要空格**。本机 Vite 常见：`http://localhost:5173`（若只用 `.env.render.local` 配了线上 Vercel，**务必把 localhost 一并写上**，否则本地登录会 CORS 预检失败）。未设时 compose 默认带 5173/5174。 |

密码未进容器时，日志里常见：  
`The server requested SCRAM-based authentication, but no password was provided.`

---

## 3. Docker Compose：正确启动命令

**要点**：`docker-compose.yml` 里的 **`${SPRING_DATASOURCE_PASSWORD}`**、**`${JWT_SECRET}`** 等，由 Compose 在解析文件时替换，来源优先级为：**当前 shell 环境变量** → **你用 `--env-file` 指定的文件** → **同目录默认文件 `.env`**（若存在）。  
与 **`.env.render.local`** 里变量名一致时，直接用下面 **方式 A** 即可。

### 方式 A：使用 `.env.render.local`（推荐，与 Render 清单一致）

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

你的 **`.env.render.local`** 里已有 **`SPRING_DATASOURCE_PASSWORD`**、**`JWT_SECRET`**、**`APP_CORS_ALLOWED_ORIGINS`** 时，会覆盖 compose 里对应项的占位符；**`SPRING_PROFILES_ACTIVE`** 在 compose 里已写死为 **`cloud`**，与文件里一致即可。

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

## 4. 可选：本机 Gradle + 远端库（`bootRun`）

**`gradlew bootRun` 不会自动读取** 仓库里的 `.env` / `.env.render.local`。请用 **IDE 的 EnvFile / Run Configuration**，或先在终端里把变量写进当前进程再启动。

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
