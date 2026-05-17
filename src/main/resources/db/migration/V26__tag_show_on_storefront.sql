-- 标签是否在商城前台展示（筛选器、商品详情标签等）；与 is_active（停用）独立
ALTER TABLE t_tags ADD COLUMN IF NOT EXISTS show_on_storefront BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE t_tags SET show_on_storefront = TRUE WHERE show_on_storefront IS NULL;
