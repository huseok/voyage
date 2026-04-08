-- V2 初始化管理员账号（默认仅用于本地开发）
-- 账号：admin@voyage.local
-- 密码：Admin@123456
-- BCrypt 哈希可在生产环境替换。

insert into t_users (email, password_hash, name, role, status)
values (
  'admin@voyage.local',
  '$2a$10$0M6Q5S8wzqbz5aYxM7b1eeSlW77H4CSkiqhSSjMBDlNIQP5CKEM5m',
  'System Admin',
  'ADMIN',
  'ACTIVE'
)
on conflict (email) do nothing;
