# Nginx 配置示例（CHZautokeys）

完整说明与 **模式 A / B 切换步骤** 见 **[`../DOMAIN_SETUP.md` § 两种域名模式](../DOMAIN_SETUP.md#两种域名模式对照与怎么改)**。

## 文件对照

| 文件 | 模式 | 用户访问 `.cyou` 时 |
|------|------|---------------------|
| [`globuy.conf.example`](./globuy.conf.example) | 仅 HTTP（签证书前） | 同内容，不 301（无 443） |
| [`globuy.conf.https.example`](./globuy.conf.https.example) | **A：跳转主域** | **301 → `https://chzautokeys.com`** |
| [`globuy.conf.https.dual-domain.example`](./globuy.conf.https.dual-domain.example) | **B：双域并列** | **留在 `https://chzautokeys.cyou`** |

## 怎么改（服务器上二选一）

**改成跳转主域（A）：**

```bash
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.https.example /etc/nginx/sites-available/globuy
# env.backend: APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com
sudo nginx -t && sudo systemctl reload nginx
# docker-compose ... restart voyage-api
```

**改成双域并列（B）：**

```bash
sudo cp /opt/globuy/repo/voyage/deploy/aliyun/nginx/globuy.conf.https.dual-domain.example /etc/nginx/sites-available/globuy
# env.backend: APP_CORS_ALLOWED_ORIGINS=https://chzautokeys.com,https://chzautokeys.cyou
sudo nginx -t && sudo systemctl reload nginx
# docker-compose ... restart voyage-api
```

**勿再使用** 旧版 `server_name _;` 且无 `acme-challenge` 的配置。
