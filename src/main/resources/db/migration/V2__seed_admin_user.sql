-- V2 初始化管理员账号（默认仅用于本地开发）
-- 账号：admin@voyage.local
-- 密码明文：Admin@123456
-- BCrypt（$2b$10），与 Spring Security BCryptPasswordEncoder 校验一致；上线可替换。

insert into t_users (email, password_hash, name, role, status)
values (
  'admin@voyage.local',
  '$2b$10$kXRYfWNApj0moXpo3.Wd1Oso.HZ6.VTWo7NVZii6i1Ag87wLK.cQG',
  'System Admin',
  'ADMIN',
  'ACTIVE'
)
on conflict (email) do nothing;

insert into t_users (email, password_hash, name, role, status)
values (
  'admin2@voyage.local',
  '$2b$10$kXRYfWNApj0moXpo3.Wd1Oso.HZ6.VTWo7NVZii6i1Ag87wLK.cQG',
  'System Admin 2',
  'ADMIN',
  'ACTIVE'
)
on conflict (email) do nothing;
