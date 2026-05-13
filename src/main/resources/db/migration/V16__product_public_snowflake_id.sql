-- 对外商品主键：雪花 ID 十进制字符串（避免 JS Number 精度问题）
ALTER TABLE t_products ADD COLUMN IF NOT EXISTS public_id VARCHAR(24);
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_products_public_id ON t_products (public_id) WHERE public_id IS NOT NULL;
