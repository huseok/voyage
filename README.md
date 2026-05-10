# Voyage（CHZfobkey 后端）

Kotlin + Spring Boot。**主路径**：远端 PostgreSQL + **Docker**；**可选**：本机 Postgres（Docker **`--profile local-db`**，见 **`DEPLOY.md` §3.1**）或本机 **`gradlew bootRun`**（见 **`DEPLOY.md` §4**）。同仓库前端：**`../foreign-trade-shop/`**。

阿里云单机（Ubuntu、固定数据目录、Nginx 同域反代）：见仓库根目录 **`deploy/aliyun/README.md`**。远端库连接串请用环境变量注入（**`application-cloud.yaml`** 已与示例 Supabase 默认值脱钩）。

---

## 本地联调地址（Docker 映射默认端口时）

| 用途 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| 买家端（前端） | http://localhost:5173 |
| 管理后台登录 | http://localhost:5173/admin/login |

---

## 启动后端

**Docker（推荐）** — 用 **`.env.render.local`**（与 Render 一致）时（详见 **`DEPLOY.md` §3 方式 A**）：

```powershell
cd <本仓库>\voyage
docker compose --env-file .env.render.local up -d --build
```

若改用默认 **`.env`**，则 **`docker compose up -d --build`** 即可。

**本机 Postgres（Docker）**：见 **`DEPLOY.md` §3.1**（`--profile local-db` + **`.env.docker.local`**）。

**本机 Gradle（可选）** — `bootRun` 不自动读 env 文件，用 IDE 或 PowerShell 注入变量后再 **`.\gradlew.bat bootRun`**，见 **`DEPLOY.md` §4**。

---

## 开发种子账号（Flyway，勿用于生产）

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | `admin@voyage.local` | `Admin@123456` |
| 管理员（备用） | `admin2@voyage.local` | `Admin@123456` |
| 普通用户 | `buyer1@voyage.local` / `buyer2@voyage.local` | `Admin@123456` |

后台：**`/admin/login`**，须管理员账号。

---

## 其它文档

| 文档 | 内容 |
|------|------|
| **`DEPLOY.md`** | 环境变量、Docker、可选 `bootRun`、云上部署 |
| **`../foreign-trade-shop/INTEGRATION.md`** | 前端 `VITE_API_BASE_URL` 与接口说明 |
| **`../GLOBUY_ROLLOUT_AND_TODO.md`** | 媒体上传、商品图、设计稿落地进度与待办 |

**本地媒体**：上传写入 `voyage.media.storage-root`（默认 `./data/media`），公开访问路径为 `/media/...`（见仓库根目录 rollout 文档）。

代码：`src/main/kotlin`；迁移：`src/main/resources/db/migration/`。
