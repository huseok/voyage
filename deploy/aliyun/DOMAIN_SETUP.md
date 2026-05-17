# CHZautokeys 域名配置（chzautokeys.com / chzautokeys.cyou）

在**域名注册商**与**阿里云 ECS**上完成下列步骤后，站点可通过自定义域名访问；后端 CORS 需与浏览器地址栏 **Origin** 一致。

## 1. DNS 解析（在域名控制台）

将以下记录指向**同一台**已部署 globuy 栈的服务器 **公网 IPv4**（A 记录）：

| 主机记录 | 类型 | 记录值 | 说明 |
|----------|------|--------|------|
| `@` | A | `<ECS 公网 IP>` | `chzautokeys.com`、`chzautokeys.cyou` 各填一条 |
| `www` | A 或 CNAME | 同上或 `@` | `www.chzautokeys.com`、`www.chzautokeys.cyou` |

- **.com** 与 **.cyou** 是**两个独立 zone**，需在各自注册商面板分别添加。
- 生效时间通常几分钟到 48 小时；可用 `ping chzautokeys.com` 或 `dig +short chzautokeys.com A` 核对。

## 2. 云安全组

入方向放行：**TCP 80**、**TCP 443**（HTTPS 启用后）。仍**不要**对 `0.0.0.0/0` 开放 **5432**、**8080**。

## 3. Nginx（HTTP 先通站）

在服务器上：

```bash
sudo mkdir -p /var/www/certbot
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.example /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

浏览器访问 `http://chzautokeys.com` 应能看到前端（需已 `rsync` 构建产物到 `/opt/globuy/www/frontend`）。

## 4. HTTPS（Let's Encrypt）

```bash
sudo apt-get update && sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot certonly --webroot -w /var/www/certbot \
  -d chzautokeys.com -d www.chzautokeys.com \
  -d chzautokeys.cyou -d www.chzautokeys.cyou \
  --email 你的邮箱 --agree-tos --no-eff-email
```

证书签发成功后，改用 HTTPS 配置（非 `chzautokeys.com` 的主机名会 301 到主域）：

```bash
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.https.example /etc/nginx/sites-available/globuy
sudo nginx -t && sudo systemctl reload nginx
```

续期：`sudo certbot renew --dry-run`（可配合 cron / systemd timer）。

## 5. 后端 CORS（必做）

编辑 `/opt/globuy/config/env.backend`，将 `APP_CORS_ALLOWED_ORIGINS` 设为浏览器实际访问的 **https** 源（英文逗号、无空格）：

```bash
# 若已 301 统一到 chzautokeys.com，通常只需主域：
APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com

# 过渡期仍用 http 调试时可临时并列（上线后去掉 http）：
# APP_CORS_ALLOWED_ORIGINS=http://chzautokeys.com,https://chzautokeys.com
```

重启 API：

```bash
cd /opt/globuy/repo/voyage
DOCKER_BUILDKIT=0 docker compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend restart voyage-api
```

## 6. 前端构建

同域 Nginx 反代 `/api` 时，`foreign-trade-shop/.env.production` 中 **`VITE_API_BASE_URL` 留空**，然后 `npm run build` 并同步 `dist/`。

## 7. 验收

| 检查项 | 期望 |
|--------|------|
| `https://chzautokeys.com` | 首页正常 |
| `https://www.chzautokeys.com` | 301 到 `https://chzautokeys.com` |
| `https://chzautokeys.cyou` | 301 到 `https://chzautokeys.com`（与 https 示例一致时） |
| 登录 / 下单 | 浏览器控制台无 CORS 错误 |
| `curl -sS https://chzautokeys.com/api/v1/tags \| head` | 返回 JSON |

若希望 **.cyou 与 .com 并列展示**（不 301），可改 `globuy.conf.https.example` 去掉 `if ($host != …)`，并把 CORS 写上两个 https 源。
