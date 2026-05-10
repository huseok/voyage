# 阿里云 Ubuntu 单机部署（文档在后端仓库 `voyage/deploy/aliyun`）

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
docker compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend up -d --build
```

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
