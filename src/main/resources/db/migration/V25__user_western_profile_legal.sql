-- 用户资料：欧美式名/姓拆分、公司名、注册时同意的法律条款版本与时间
ALTER TABLE t_users ADD COLUMN IF NOT EXISTS first_name VARCHAR(80);
ALTER TABLE t_users ADD COLUMN IF NOT EXISTS last_name VARCHAR(80);
ALTER TABLE t_users ADD COLUMN IF NOT EXISTS company_name VARCHAR(200);
ALTER TABLE t_users ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE t_users ADD COLUMN IF NOT EXISTS terms_version VARCHAR(32);
ALTER TABLE t_users ADD COLUMN IF NOT EXISTS privacy_version VARCHAR(32);

-- 历史数据：将原 name 迁入 first_name
UPDATE t_users
SET first_name = COALESCE(NULLIF(TRIM(first_name), ''), TRIM(name)),
    last_name = COALESCE(NULLIF(TRIM(last_name), ''), '')
WHERE first_name IS NULL OR TRIM(first_name) = '';

ALTER TABLE t_users ALTER COLUMN first_name SET DEFAULT '';
ALTER TABLE t_users ALTER COLUMN last_name SET DEFAULT '';
UPDATE t_users SET first_name = '' WHERE first_name IS NULL;
UPDATE t_users SET last_name = '' WHERE last_name IS NULL;
ALTER TABLE t_users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE t_users ALTER COLUMN last_name SET NOT NULL;

-- salutation 保留为可选称谓（Mr / Ms 等），不再强制
