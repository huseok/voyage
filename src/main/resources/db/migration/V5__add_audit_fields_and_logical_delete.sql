-- V5: 全量补齐审计字段与逻辑删除字段
-- 目标：所有业务表具备 created_at / updated_at / is_deleted（缺失则补齐）

alter table t_users add column if not exists is_deleted boolean not null default false;
alter table t_products add column if not exists is_deleted boolean not null default false;
alter table t_cart_items add column if not exists is_deleted boolean not null default false;
alter table t_orders add column if not exists is_deleted boolean not null default false;
alter table t_order_items add column if not exists updated_at timestamptz not null default now();
alter table t_order_items add column if not exists is_deleted boolean not null default false;
alter table t_after_sales add column if not exists is_deleted boolean not null default false;

alter table t_categories add column if not exists is_deleted boolean not null default false;
alter table t_product_options add column if not exists created_at timestamptz not null default now();
alter table t_product_options add column if not exists updated_at timestamptz not null default now();
alter table t_product_options add column if not exists is_deleted boolean not null default false;
alter table t_product_skus add column if not exists is_deleted boolean not null default false;

alter table t_shipping_templates add column if not exists is_deleted boolean not null default false;
alter table t_shipping_template_rules add column if not exists created_at timestamptz not null default now();
alter table t_shipping_template_rules add column if not exists updated_at timestamptz not null default now();
alter table t_shipping_template_rules add column if not exists is_deleted boolean not null default false;

alter table t_user_addresses add column if not exists is_deleted boolean not null default false;
alter table t_browse_histories add column if not exists created_at timestamptz not null default now();
alter table t_browse_histories add column if not exists updated_at timestamptz not null default now();
alter table t_browse_histories add column if not exists is_deleted boolean not null default false;

alter table t_site_contents add column if not exists is_deleted boolean not null default false;
alter table t_business_cooperations add column if not exists is_deleted boolean not null default false;

alter table t_dict_types add column if not exists is_deleted boolean not null default false;
alter table t_dict_items add column if not exists is_deleted boolean not null default false;

alter table t_audit_logs add column if not exists updated_at timestamptz not null default now();
alter table t_audit_logs add column if not exists is_deleted boolean not null default false;
alter table t_order_status_histories add column if not exists created_at timestamptz not null default now();
alter table t_order_status_histories add column if not exists updated_at timestamptz not null default now();
alter table t_order_status_histories add column if not exists is_deleted boolean not null default false;
alter table t_price_change_histories add column if not exists created_at timestamptz not null default now();
alter table t_price_change_histories add column if not exists updated_at timestamptz not null default now();
alter table t_price_change_histories add column if not exists is_deleted boolean not null default false;
