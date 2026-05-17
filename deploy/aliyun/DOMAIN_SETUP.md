# CHZautokeys 域名与 HTTPS（chzautokeys.com / chzautokeys.cyou）

生产站点：**https://chzautokeys.com**（主域）。证书路径：`/etc/letsencrypt/live/chzautokeys.com/`。

| 文档 | 说明 |
|------|------|
| 本文 | DNS、Nginx、证书、CORS、排错 |
| [`nginx/README.md`](./nginx/README.md) | 三份 `globuy.conf*.example` 对照 |
| [`DEPLOY_STEP_BY_STEP.md`](./DEPLOY_STEP_BY_STEP.md) | 整机部署（Docker、前端构建） |
| 下文 **§ 两种域名模式** | **主域跳转** vs **双域并列**（怎么改） |
| 下文 **§ 生产上线实录** | 2026-05-17 阿里云实操记录 |

---

## 两种域名模式（对照与怎么改）

系统有两个顶级域名：`chzautokeys.com`、`chzautokeys.cyou`。HTTPS 下可选两种 Nginx 策略（**证书同一份**，不用重新签）。

### 模式对照

| 项目 | 模式 A：跳转主域名（当前生产默认） | 模式 B：双域名并列 |
|------|-----------------------------------|-------------------|
| **适用场景** | 对外只宣传一个网址；SEO、书签统一 | `.com`、`.cyou` 都要能独立打开、各自分享 |
| **仓库配置文件** | `nginx/globuy.conf.https.example` | `nginx/globuy.conf.https.dual-domain.example` |
| 访问 `https://chzautokeys.com` | 正常打开站点 | 正常打开站点 |
| 访问 `https://chzautokeys.cyou` | **301** → `https://chzautokeys.com` | **不跳转**，地址栏仍是 `.cyou` |
| 访问 `https://www.chzautokeys.com` 等 | **301** → `https://chzautokeys.com` | **不跳转**，地址栏仍是 `www....` |
| HTTP（80） | 一律 **301** 到 `https://chzautokeys.com` | **301** 到 `https://` + **当前访问的主机名**（`$host`） |
| **`APP_CORS_ALLOWED_ORIGINS`** | `https://chzautokeys.com` | `https://chzautokeys.com,https://chzautokeys.cyou`（用 `www` 再加对应项） |
| 前端 `VITE_API_BASE_URL` | 留空（同域反代） | 留空（同域反代） |

模式 A 里「`.cyou 打开却变成 .com`」是 **Nginx 故意写的**，不是 DNS 坏了。

### 关键配置差异（手工改 Nginx 时）

**模式 A**（`globuy.conf.https.example`）在 `server { listen 443 ... }` 内有：

```nginx
# HTTP
location / {
    return 301 https://chzautokeys.com$request_uri;
}

# HTTPS
if ($host != chzautokeys.com) {
    return 301 https://chzautokeys.com$request_uri;
}
```

**模式 B**（`globuy.conf.https.dual-domain.example`）：

- **删掉** 上面整段 `if ($host != chzautokeys.com) { ... }`
- HTTP 的 `location /` 改为：

```nginx
location / {
    return 301 https://$host$request_uri;
}
```

443 里只保留 `root`、`location /`、`/api/`、`/media/`，**不要**再写按 `$host` 301 到 `.com` 的逻辑。

---

### 改成「跳转主域名」（模式 A）

在服务器执行（路径按你的 voyage 仓库）：

```bash
cd /opt/globuy/repo/voyage

sudo cp deploy/aliyun/nginx/globuy.conf.https.example /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/globuy
sudo nginx -t && sudo systemctl reload nginx
```

编辑 CORS：

```bash
sudo nano /opt/globuy/config/env.backend
```

设为（一行、无空格、无引号）：

```bash
APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com
```

重启后端：

```bash
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
export DOCKER_BUILDKIT=0
docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend restart voyage-api
```

**验收：**

```bash
curl -sSI https://chzautokeys.cyou/ | grep -i '^location:'
# 期望：location: https://chzautokeys.com/
```

浏览器打开 `https://chzautokeys.cyou`，地址栏应变为 `https://chzautokeys.com`。

---

### 改成「双域名并列」（模式 B）

```bash
cd /opt/globuy/repo/voyage

sudo cp deploy/aliyun/nginx/globuy.conf.https.dual-domain.example /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/globuy
sudo nginx -t && sudo systemctl reload nginx
```

编辑 CORS（**两个 https 源都要写**，否则在 `.cyou` 登录会 CORS 失败）：

```bash
APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com,https://chzautokeys.cyou
```

若也要用 `www`：

```bash
APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com,https://www.chzautokeys.com,https://chzautokeys.cyou,https://www.chzautokeys.cyou
```

重启后端（同上 `docker-compose ... restart voyage-api`）。

**验收：**

```bash
curl -sSI https://chzautokeys.cyou/ | grep -i '^HTTP\|^location:'
# 期望：HTTP/2 200（或 200），且没有 location 指向 .com

curl -sS -o /dev/null -w "%{http_code}\n" https://chzautokeys.cyou/api/v1/tags
# 期望：200
```

浏览器：`https://chzautokeys.com` 与 `https://chzautokeys.cyou` 地址栏应**各自保持**，互不跳转。

---

### 切换时注意

1. **改 Nginx 后必须改 CORS 并 restart `voyage-api`**，否则某一域名下 API 会被浏览器拦截。  
2. **证书不用换**：两种模式都用 `/etc/letsencrypt/live/chzautokeys.com/`（已含四个主机名）。  
3. 切换前可备份：`sudo cp /etc/nginx/sites-available/globuy /etc/nginx/sites-available/globuy.bak.$(date +%F)`  
4. 本地仓库示例在 `voyage/deploy/aliyun/nginx/`，`git pull` 后再 `cp` 到服务器。

---

## 生产上线实录（2026-05-17）

在阿里云 ECS（示例主机 `iZj6chqfs1rhe3mdzypo5oZ`）完成域名与 HTTPS 上线的真实步骤与结论，便于日后复现或排错。

### 已购域名与 DNS

| 域名 | 说明 |
|------|------|
| `chzautokeys.com` | 主站（canonical） |
| `chzautokeys.cyou` | 备用；当前 Nginx **会 301 到 .com**（见 § 主域统一） |

两域名的 `@`、`www` 均 **A 记录** 指向同一 ECS 公网 IP。安全组放行 **80、443**；不对公网开放 **5432、8080**。

### 当时的问题与处理

| 现象 | 原因 | 处理 |
|------|------|------|
| `certbot --webroot` 报 `unauthorized`，详情里是 `<!doctype html>` | `/etc/nginx/sites-enabled/globuy` 仍是旧模板：`server_name _;`，无 `/.well-known/acme-challenge/`，校验 URL 被 SPA `try_files` 成 `index.html` | 见 § certbot 返回 index.html；或先用 **standalone** 签证书 |
| `curl http://chzautokeys.com/.well-known/.../ping-test` 返回 HTML | 同上 | 更新 Nginx 示例并 reload；或 standalone |
| `certbot install` → `Could not install certificate` | 线上尚无匹配域名的 `server_name` 块 | **可忽略**；证书已有后 **手工** 套用 `globuy.conf.https.example` |
| `docker compose -f ...` → `unknown shorthand flag: 'f'` | 未装 Compose V2 插件 | 使用 **`docker-compose`**（连字符），见 § 重启后端 |
| `restart` 后 HTTPS `502` | Spring Boot 刚启动（容器 `Up` 十几秒） | 等 30～90 秒；`curl http://127.0.0.1:8080/api/v1/tags`；`docker logs globuy-api` |
| 访问 `.cyou` 跳到 `.com` | `globuy.conf.https.example` 中故意配置 | 见 § 主域统一 / § 双域名模式 |

### 证书签发（standalone，已通过）

```bash
sudo systemctl stop nginx
sudo certbot certonly --standalone \
  -d chzautokeys.com -d www.chzautokeys.com \
  -d chzautokeys.cyou -d www.chzautokeys.cyou \
  --email <你的邮箱> --agree-tos --no-eff-email
sudo systemctl start nginx
```

证书约 **90 天**有效；续期见 § 证书续期。

### HTTPS Nginx（当前生产逻辑）

1. 准备 SSL 辅助文件（standalone 后可能缺失）：

```bash
sudo test -f /etc/letsencrypt/options-ssl-nginx.conf || \
  sudo cp /usr/lib/python3/dist-packages/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf \
    /etc/letsencrypt/options-ssl-nginx.conf
sudo test -f /etc/letsencrypt/ssl-dhparams.pem || \
  sudo openssl dhparam -out /etc/letsencrypt/ssl-dhparams.pem 2048
```

2. 启用 HTTPS 配置（主域统一，**.cyou → .com**）：

```bash
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.https.example \
  /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/globuy
sudo nginx -t && sudo systemctl reload nginx
```

3. 验收：`curl -sS -o /dev/null -w "%{http_code}\n" https://chzautokeys.com/api/v1/tags` → **200**

### CORS 与后端重启

`/opt/globuy/config/env.backend`：

```bash
APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com
```

```bash
cd /opt/globuy/repo/voyage
unset DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD
export DOCKER_BUILDKIT=0
docker-compose -f deploy/aliyun/docker-compose.stack.yml \
  --env-file /opt/globuy/config/env.backend restart voyage-api
```

> 若已安装 Compose V2，可将 `docker-compose` 换成 `docker compose`（中间有空格）。

---

## 1. DNS 解析（在域名控制台）

| 主机记录 | 类型 | 记录值 | 说明 |
|----------|------|--------|------|
| `@` | A | `<ECS 公网 IP>` | `chzautokeys.com`、`chzautokeys.cyou` **各 zone 各配一条** |
| `www` | A 或 CNAME | 同上或 `@` | 两个域名分别配置 |

核对：`dig +short chzautokeys.com A`

## 2. 云安全组

入方向：**TCP 80**、**TCP 443**。勿对公网开放 **5432**、**8080**。

## 3. Nginx 配置文件选择

| 阶段 / 需求 | 使用文件 |
|-------------|----------|
| 首次 HTTP、签 webroot 证书前 | `nginx/globuy.conf.example` |
| 生产 HTTPS，**.cyou 跳 .com**（默认） | `nginx/globuy.conf.https.example` |
| **.com 与 .cyou 并列**，互不跳转 | `nginx/globuy.conf.https.dual-domain.example` |

```bash
sudo mkdir -p /var/www/certbot/.well-known/acme-challenge
sudo cp .../nginx/globuy.conf.<选用>.example /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/globuy
sudo nginx -t && sudo systemctl reload nginx
```

前端静态目录：`/opt/globuy/www/frontend`（`npm run build` 后 `rsync dist/`）。

## 4. HTTPS 与 Let's Encrypt

### 签发前自检（webroot 方式）

```bash
echo ok | sudo tee /var/www/certbot/.well-known/acme-challenge/ping-test
curl -sS http://chzautokeys.com/.well-known/acme-challenge/ping-test
# 必须输出 ok，不能是 HTML
```

### webroot 签发

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot certonly --webroot -w /var/www/certbot \
  -d chzautokeys.com -d www.chzautokeys.com \
  -d chzautokeys.cyou -d www.chzautokeys.cyou \
  --email 你的邮箱 --agree-tos --no-eff-email
```

### standalone 签发（站点暂不可停用时慎用；需停 Nginx 约 1 分钟）

见上文 **§ 生产上线实录**。

### 证书续期

启用 `globuy.conf.https*.example` 后，80 端口已含 `acme-challenge`：

```bash
sudo certbot renew --dry-run
```

若 renewal 仍配置为 `standalone`，可编辑 `/etc/letsencrypt/renewal/chzautokeys.com.conf`，将 `authenticator = webroot` 并设置 `webroot_path = /var/www/certbot`。

## 5. 主域统一 / 双域名（详见上文）

**§ 两种域名模式** 含对照表、验收命令与 A↔B 切换步骤。简要：

- **跳转主域**：`globuy.conf.https.example` + `APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com`
- **双域并列**：`globuy.conf.https.dual-domain.example` + CORS 含 `.com` 与 `.cyou` 两个 https 源

## 6. 后端 CORS 与重启

| 访问方式 | `APP_CORS_ALLOWED_ORIGINS` 示例 |
|----------|----------------------------------|
| 仅主域 HTTPS | `https://chzautokeys.com` |
| 双域并列 | `https://chzautokeys.com,https://chzautokeys.cyou` |
| 本地 Vite 联调 | 末尾追加 `,http://localhost:5173` |

值须与浏览器地址栏 **Origin** 完全一致（含 `https://`、无尾斜杠、逗号无空格）。改后 **restart** `voyage-api`（见 § 生产上线实录）。

## 7. 前端构建

`foreign-trade-shop/.env.production`：**`VITE_API_BASE_URL` 留空**（同域反代 `/api`），`npm run build` 后同步到 `/opt/globuy/www/frontend/`。

## 8. 验收清单

| 检查项 | 模式 A（跳转主域） | 模式 B（双域并列） |
|--------|-------------------|-------------------|
| `https://chzautokeys.com` | 首页正常 | 首页正常 |
| `https://chzautokeys.cyou` | 301 → `.com` | **200，不跳转** |
| `https://chzautokeys.com/api/v1/tags` | 200 JSON | 200 JSON |
| `https://chzautokeys.cyou/api/v1/tags` | （随 301 到 .com 后可用） | **200 JSON** |
| 登录 / 管理后台 | 无 CORS | 在 **两个域名下分别**试，均无 CORS |
| ACME `ping-test` | 纯文本 `ok` | 纯文本 `ok` |

---

## certbot 返回 index.html（`unauthorized`）

Let's Encrypt 访问校验 URL 时若得到 **前端 HTML**，说明 Nginx 未把该路径交给 `/var/www/certbot`，或仍使用 **`server_name _;`** 且无 `acme-challenge` 的旧 `globuy`。

```bash
sudo nginx -T 2>/dev/null | grep -E 'server_name|acme-challenge'
sudo grep -r 'www/frontend\|server_name' /etc/nginx/sites-enabled/ /etc/nginx/conf.d/

sudo mkdir -p /var/www/certbot/.well-known/acme-challenge
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.example \
  /etc/nginx/sites-available/globuy
sudo ln -sf /etc/nginx/sites-available/globuy /etc/nginx/sites-enabled/globuy
# 若 conf.d/default.conf 抢占：sudo mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak
sudo nginx -t && sudo systemctl reload nginx
```

`location ^~ /.well-known/acme-challenge/` 必须写在 **`location / { try_files ... }` 之前**。

---

## 重启后端（Compose 命令对照）

| 环境 | 命令 |
|------|------|
| 阿里云常见（Compose V1 独立二进制） | `docker-compose -f deploy/aliyun/docker-compose.stack.yml --env-file /opt/globuy/config/env.backend restart voyage-api` |
| 已装 Compose V2 插件 | `docker compose -f ...`（`compose` 与 `docker` 之间有空格） |

**502 Bad Gateway**：先看 `docker logs globuy-api --tail 50`，再等 30～90 秒后 `curl http://127.0.0.1:8080/api/v1/tags`。

---

## 从旧配置迁移检查表

服务器上若 `cat /etc/nginx/sites-enabled/globuy` 仍含 `server_name _;` 且无 `listen 443`，请：

1. `git pull` 后端仓库  
2. 按 §4 / §3 替换为 `globuy.conf.https.example`（或 dual-domain）  
3. 更新 `APP_CORS_ALLOWED_ORIGINS` 并 `docker-compose ... restart voyage-api`  
4. 跑 §9 验收  
