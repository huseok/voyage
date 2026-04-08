# Voyage 后端部署说明（Docker）

本文档用于把 `voyage` 后端交付给他人运行，包含三种方式：
- 方式 A：发源码 + `docker compose`（推荐）
- 方式 B：发镜像（Docker Hub / 私有仓库）
- 方式 C：离线发镜像文件（`.tar`）

---

## 0. 前置条件
- 安装 Docker Desktop（Windows/macOS）或 Docker Engine（Linux）
- 能执行 `docker` 与 `docker compose` 命令
- 端口未被占用：
  - `8080`（后端 API）
  - `5432`（PostgreSQL）

检查命令：

```bash
docker --version
docker compose version
```

---

## 1. 方式 A：发源码，别人一键跑（推荐）

### 1.1 你需要提供给对方
- `voyage` 项目完整目录（至少包含以下文件）
  - `docker-compose.yml`
  - `Dockerfile`
  - `build.gradle.kts`
  - `src/main/resources/application.yaml`
  - `src/main/resources/db/migration/*.sql`

### 1.2 对方启动命令
进入 `voyage` 目录执行：

```bash
docker compose up -d --build
```

### 1.3 访问验证
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 2. 方式 B：发镜像（Docker Hub / 私有仓库）

> 适合不想发源码，只想给可运行镜像。

### 2.1 你本地构建镜像
在 `voyage` 根目录执行：

```bash
docker build -t yourname/voyage-api:0.1.0 .
```

### 2.2 推送镜像
```bash
docker login
docker push yourname/voyage-api:0.1.0
```

### 2.3 对方修改 `docker-compose.yml`
把 `voyage-api` 服务的 `build` 替换为 `image`：

```yaml
voyage-api:
  image: yourname/voyage-api:0.1.0
  container_name: voyage-api
  restart: unless-stopped
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://db.xufviqcdhpncrdkyelrd.supabase.co:5432/postgres
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: ${SUPABASE_DB_PASSWORD}
    JWT_SECRET: change_me_to_a_long_secret_for_prod
  ports:
    - "8080:8080"
```

### 2.4 对方启动
```bash
docker compose up -d
```

---

## 3. 方式 C：离线分发镜像（无外网）

### 3.1 你导出镜像
```bash
docker build -t voyage-api:0.1.0 .
docker save -o voyage-api-0.1.0.tar voyage-api:0.1.0
```

把 `voyage-api-0.1.0.tar` 发给对方（U 盘/内网传输均可）。

### 3.2 对方导入镜像
```bash
docker load -i voyage-api-0.1.0.tar
```

### 3.3 对方 `docker-compose.yml` 使用本地镜像
```yaml
voyage-api:
  image: voyage-api:0.1.0
```

然后启动：

```bash
docker compose up -d
```

---

## 4. 默认开发配置（测试环境）

### 4.1 PostgreSQL
- Host: `localhost`
- Port: `5432`
- DB: `voyage_db`
- User: `voyage_user`
- Password: `change_me`

### 4.2 管理员账号（Flyway 初始化）
- Email: `admin@voyage.local`
- Password: `Admin@123456`

---

## 5. 常用运维命令

### 5.1 查看状态
```bash
docker compose ps
docker ps
```

### 5.2 查看日志
```bash
docker compose logs -f voyage-api
docker compose logs -f db
```

### 5.3 重启服务
```bash
docker compose restart
docker compose restart voyage-api
```

### 5.4 停止并删除（保留数据）
```bash
docker compose down
```

### 5.5 停止并删除（清空数据）
```bash
docker compose down -v
```

---

## 6. 常见问题排查

### Q1: 8080 端口被占用
改 `docker-compose.yml`：

```yaml
ports:
  - "8081:8080"
```

然后访问 `http://localhost:8081`。

### Q2: 5432 端口被占用
改 `docker-compose.yml`：

```yaml
ports:
  - "5433:5432"
```

并同步修改外部数据库客户端连接端口。

### Q3: Docker Hub 拉镜像失败（网络问题）
- 重试 `docker pull`
- 配置镜像加速器
- 先用离线镜像方案（方式 C）

### Q4: 需要全新初始化数据库
```bash
docker compose down -v
docker compose up -d --build
```

---

## 7. 交付建议
- 开发/测试：可保留本文默认密码，便于联调
- 准生产/生产：必须替换
  - `JWT_SECRET`
  - 数据库账号密码
  - 管理员初始密码
- 建议新增 `.env` 管理敏感配置，并避免提交到 Git
