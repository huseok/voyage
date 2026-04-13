-- V6: 全库补齐创建人/修改人/删除元数据
-- 统一字段约定：
-- created_by BIGINT, updated_by BIGINT, deleted_by BIGINT, deleted_at TIMESTAMPTZ
-- created_at / updated_at / is_deleted 由 V5 补齐，此处兜底再补一次

-- 用户与交易核心表
alter table t_users add column if not exists created_by bigint;
alter table t_users add column if not exists updated_by bigint;
alter table t_users add column if not exists deleted_by bigint;
alter table t_users add column if not exists deleted_at timestamptz;
alter table t_users add column if not exists is_deleted boolean not null default false;
alter table t_users add column if not exists created_at timestamptz not null default now();
alter table t_users add column if not exists updated_at timestamptz not null default now();

alter table t_products add column if not exists created_by bigint;
alter table t_products add column if not exists updated_by bigint;
alter table t_products add column if not exists deleted_by bigint;
alter table t_products add column if not exists deleted_at timestamptz;
alter table t_products add column if not exists is_deleted boolean not null default false;
alter table t_products add column if not exists created_at timestamptz not null default now();
alter table t_products add column if not exists updated_at timestamptz not null default now();

alter table t_cart_items add column if not exists created_by bigint;
alter table t_cart_items add column if not exists updated_by bigint;
alter table t_cart_items add column if not exists deleted_by bigint;
alter table t_cart_items add column if not exists deleted_at timestamptz;
alter table t_cart_items add column if not exists is_deleted boolean not null default false;
alter table t_cart_items add column if not exists created_at timestamptz not null default now();
alter table t_cart_items add column if not exists updated_at timestamptz not null default now();

alter table t_orders add column if not exists created_by bigint;
alter table t_orders add column if not exists updated_by bigint;
alter table t_orders add column if not exists deleted_by bigint;
alter table t_orders add column if not exists deleted_at timestamptz;
alter table t_orders add column if not exists is_deleted boolean not null default false;
alter table t_orders add column if not exists created_at timestamptz not null default now();
alter table t_orders add column if not exists updated_at timestamptz not null default now();

alter table t_order_items add column if not exists created_by bigint;
alter table t_order_items add column if not exists updated_by bigint;
alter table t_order_items add column if not exists deleted_by bigint;
alter table t_order_items add column if not exists deleted_at timestamptz;
alter table t_order_items add column if not exists is_deleted boolean not null default false;
alter table t_order_items add column if not exists created_at timestamptz not null default now();
alter table t_order_items add column if not exists updated_at timestamptz not null default now();

alter table t_after_sales add column if not exists created_by bigint;
alter table t_after_sales add column if not exists updated_by bigint;
alter table t_after_sales add column if not exists deleted_by bigint;
alter table t_after_sales add column if not exists deleted_at timestamptz;
alter table t_after_sales add column if not exists is_deleted boolean not null default false;
alter table t_after_sales add column if not exists created_at timestamptz not null default now();
alter table t_after_sales add column if not exists updated_at timestamptz not null default now();

-- 新增域模型表
alter table t_categories add column if not exists created_by bigint;
alter table t_categories add column if not exists updated_by bigint;
alter table t_categories add column if not exists deleted_by bigint;
alter table t_categories add column if not exists deleted_at timestamptz;
alter table t_categories add column if not exists is_deleted boolean not null default false;
alter table t_categories add column if not exists created_at timestamptz not null default now();
alter table t_categories add column if not exists updated_at timestamptz not null default now();

alter table t_product_options add column if not exists created_by bigint;
alter table t_product_options add column if not exists updated_by bigint;
alter table t_product_options add column if not exists deleted_by bigint;
alter table t_product_options add column if not exists deleted_at timestamptz;
alter table t_product_options add column if not exists is_deleted boolean not null default false;
alter table t_product_options add column if not exists created_at timestamptz not null default now();
alter table t_product_options add column if not exists updated_at timestamptz not null default now();

alter table t_product_skus add column if not exists created_by bigint;
alter table t_product_skus add column if not exists updated_by bigint;
alter table t_product_skus add column if not exists deleted_by bigint;
alter table t_product_skus add column if not exists deleted_at timestamptz;
alter table t_product_skus add column if not exists is_deleted boolean not null default false;
alter table t_product_skus add column if not exists created_at timestamptz not null default now();
alter table t_product_skus add column if not exists updated_at timestamptz not null default now();

alter table t_shipping_templates add column if not exists created_by bigint;
alter table t_shipping_templates add column if not exists updated_by bigint;
alter table t_shipping_templates add column if not exists deleted_by bigint;
alter table t_shipping_templates add column if not exists deleted_at timestamptz;
alter table t_shipping_templates add column if not exists is_deleted boolean not null default false;
alter table t_shipping_templates add column if not exists created_at timestamptz not null default now();
alter table t_shipping_templates add column if not exists updated_at timestamptz not null default now();

alter table t_shipping_template_rules add column if not exists created_by bigint;
alter table t_shipping_template_rules add column if not exists updated_by bigint;
alter table t_shipping_template_rules add column if not exists deleted_by bigint;
alter table t_shipping_template_rules add column if not exists deleted_at timestamptz;
alter table t_shipping_template_rules add column if not exists is_deleted boolean not null default false;
alter table t_shipping_template_rules add column if not exists created_at timestamptz not null default now();
alter table t_shipping_template_rules add column if not exists updated_at timestamptz not null default now();

alter table t_user_addresses add column if not exists created_by bigint;
alter table t_user_addresses add column if not exists updated_by bigint;
alter table t_user_addresses add column if not exists deleted_by bigint;
alter table t_user_addresses add column if not exists deleted_at timestamptz;
alter table t_user_addresses add column if not exists is_deleted boolean not null default false;
alter table t_user_addresses add column if not exists created_at timestamptz not null default now();
alter table t_user_addresses add column if not exists updated_at timestamptz not null default now();

alter table t_browse_histories add column if not exists created_by bigint;
alter table t_browse_histories add column if not exists updated_by bigint;
alter table t_browse_histories add column if not exists deleted_by bigint;
alter table t_browse_histories add column if not exists deleted_at timestamptz;
alter table t_browse_histories add column if not exists is_deleted boolean not null default false;
alter table t_browse_histories add column if not exists created_at timestamptz not null default now();
alter table t_browse_histories add column if not exists updated_at timestamptz not null default now();

alter table t_site_contents add column if not exists created_by bigint;
alter table t_site_contents add column if not exists updated_by bigint;
alter table t_site_contents add column if not exists deleted_by bigint;
alter table t_site_contents add column if not exists deleted_at timestamptz;
alter table t_site_contents add column if not exists is_deleted boolean not null default false;
alter table t_site_contents add column if not exists created_at timestamptz not null default now();
alter table t_site_contents add column if not exists updated_at timestamptz not null default now();

alter table t_business_cooperations add column if not exists created_by bigint;
alter table t_business_cooperations add column if not exists updated_by bigint;
alter table t_business_cooperations add column if not exists deleted_by bigint;
alter table t_business_cooperations add column if not exists deleted_at timestamptz;
alter table t_business_cooperations add column if not exists is_deleted boolean not null default false;
alter table t_business_cooperations add column if not exists created_at timestamptz not null default now();
alter table t_business_cooperations add column if not exists updated_at timestamptz not null default now();

alter table t_dict_types add column if not exists created_by bigint;
alter table t_dict_types add column if not exists updated_by bigint;
alter table t_dict_types add column if not exists deleted_by bigint;
alter table t_dict_types add column if not exists deleted_at timestamptz;
alter table t_dict_types add column if not exists is_deleted boolean not null default false;
alter table t_dict_types add column if not exists created_at timestamptz not null default now();
alter table t_dict_types add column if not exists updated_at timestamptz not null default now();

alter table t_dict_items add column if not exists created_by bigint;
alter table t_dict_items add column if not exists updated_by bigint;
alter table t_dict_items add column if not exists deleted_by bigint;
alter table t_dict_items add column if not exists deleted_at timestamptz;
alter table t_dict_items add column if not exists is_deleted boolean not null default false;
alter table t_dict_items add column if not exists created_at timestamptz not null default now();
alter table t_dict_items add column if not exists updated_at timestamptz not null default now();

alter table t_audit_logs add column if not exists created_by bigint;
alter table t_audit_logs add column if not exists updated_by bigint;
alter table t_audit_logs add column if not exists deleted_by bigint;
alter table t_audit_logs add column if not exists deleted_at timestamptz;
alter table t_audit_logs add column if not exists is_deleted boolean not null default false;
alter table t_audit_logs add column if not exists created_at timestamptz not null default now();
alter table t_audit_logs add column if not exists updated_at timestamptz not null default now();

alter table t_order_status_histories add column if not exists created_by bigint;
alter table t_order_status_histories add column if not exists updated_by bigint;
alter table t_order_status_histories add column if not exists deleted_by bigint;
alter table t_order_status_histories add column if not exists deleted_at timestamptz;
alter table t_order_status_histories add column if not exists is_deleted boolean not null default false;
alter table t_order_status_histories add column if not exists created_at timestamptz not null default now();
alter table t_order_status_histories add column if not exists updated_at timestamptz not null default now();

alter table t_price_change_histories add column if not exists created_by bigint;
alter table t_price_change_histories add column if not exists updated_by bigint;
alter table t_price_change_histories add column if not exists deleted_by bigint;
alter table t_price_change_histories add column if not exists deleted_at timestamptz;
alter table t_price_change_histories add column if not exists is_deleted boolean not null default false;
alter table t_price_change_histories add column if not exists created_at timestamptz not null default now();
alter table t_price_change_histories add column if not exists updated_at timestamptz not null default now();
