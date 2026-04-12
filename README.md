# Voyage（Globuy 后端）

Kotlin + Spring Boot。**主路径**：远端 PostgreSQL + **Docker**；**可选**：本机 **`gradlew bootRun`** 直连同一远端库（见 **`DEPLOY.md` §4**）。同仓库前端：**`../foreign-trade-shop/`**。

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

代码：`src/main/kotlin`；迁移：`src/main/resources/db/migration/`。
