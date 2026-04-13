-- V4: 领域扩展（分类、SKU、运费、用户中心、CMS、字典、审计与历史）
-- 目标：在不破坏既有订单/购物车/商品主链路的前提下，补齐可扩展的电商基础能力。

-- ========== 商品与分类 ==========
create table if not exists t_categories (
  id bigserial primary key,
  parent_id bigint references t_categories(id),
  name varchar(120) not null,
  code varchar(80) not null unique,
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table t_products
  add column if not exists category_id bigint references t_categories(id),
  add column if not exists weight_kg numeric(10,3),
  add column if not exists shipping_template_id bigint;

create table if not exists t_product_options (
  id bigserial primary key,
  product_id bigint not null references t_products(id) on delete cascade,
  option_name varchar(80) not null,
  option_value varchar(120) not null,
  sort_no int not null default 0
);

create table if not exists t_product_skus (
  id bigserial primary key,
  product_id bigint not null references t_products(id) on delete cascade,
  sku_code varchar(64) not null unique,
  attr_json text not null,
  sale_price numeric(12,2) not null,
  stock_qty int not null default 0,
  weight_kg numeric(10,3),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ========== 运费模板 ==========
create table if not exists t_shipping_templates (
  id bigserial primary key,
  template_name varchar(120) not null,
  billing_mode varchar(30) not null default 'BY_WEIGHT',
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_shipping_template_rules (
  id bigserial primary key,
  template_id bigint not null references t_shipping_templates(id) on delete cascade,
  region_code varchar(40) not null default 'GLOBAL',
  first_weight_kg numeric(10,3) not null default 0,
  first_fee numeric(12,2) not null default 0,
  additional_weight_kg numeric(10,3) not null default 1,
  additional_fee numeric(12,2) not null default 0,
  sort_no int not null default 0
);

alter table t_products
  add constraint fk_t_products_shipping_template
  foreign key (shipping_template_id) references t_shipping_templates(id);

-- ========== 用户中心 ==========
create table if not exists t_user_addresses (
  id bigserial primary key,
  user_id bigint not null references t_users(id),
  receiver_name varchar(100) not null,
  receiver_phone varchar(30) not null,
  country varchar(60) not null,
  address_line varchar(255) not null,
  postal_code varchar(20),
  is_default boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_browse_histories (
  id bigserial primary key,
  user_id bigint not null references t_users(id),
  product_id bigint not null references t_products(id),
  viewed_at timestamptz not null default now()
);

-- ========== CMS 与商务合作 ==========
create table if not exists t_site_contents (
  id bigserial primary key,
  content_key varchar(80) not null unique,
  content_type varchar(30) not null,
  title varchar(255),
  subtitle varchar(500),
  body text,
  image_url varchar(500),
  action_url varchar(500),
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_business_cooperations (
  id bigserial primary key,
  email varchar(255) not null,
  contact_name varchar(100),
  whatsapp varchar(60),
  wechat varchar(60),
  content text not null,
  status varchar(30) not null default 'NEW',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ========== 字典 ==========
create table if not exists t_dict_types (
  id bigserial primary key,
  dict_code varchar(80) not null unique,
  dict_name varchar(120) not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_dict_items (
  id bigserial primary key,
  dict_type_id bigint not null references t_dict_types(id) on delete cascade,
  item_code varchar(80) not null,
  item_label varchar(120) not null,
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_t_dict_items_type_code unique (dict_type_id, item_code)
);

-- ========== 审计与历史 ==========
create table if not exists t_audit_logs (
  id bigserial primary key,
  actor_user_id bigint references t_users(id),
  actor_role varchar(30),
  action_code varchar(80) not null,
  entity_type varchar(80) not null,
  entity_id varchar(80) not null,
  detail_json text,
  created_at timestamptz not null default now()
);

create table if not exists t_order_status_histories (
  id bigserial primary key,
  order_id bigint not null references t_orders(id) on delete cascade,
  from_status varchar(32),
  to_status varchar(32) not null,
  changed_by bigint references t_users(id),
  changed_at timestamptz not null default now(),
  remark varchar(255)
);

create table if not exists t_price_change_histories (
  id bigserial primary key,
  product_id bigint not null references t_products(id) on delete cascade,
  sku_id bigint references t_product_skus(id) on delete set null,
  old_price numeric(12,2) not null,
  new_price numeric(12,2) not null,
  changed_by bigint references t_users(id),
  changed_at timestamptz not null default now(),
  remark varchar(255)
);

-- ========== 初始化字典 ==========
insert into t_dict_types (dict_code, dict_name)
values
  ('ORDER_STATUS', '订单状态'),
  ('AFTER_SALE_STATUS', '售后状态'),
  ('COOP_STATUS', '合作线索状态'),
  ('SHIPPING_BILLING_MODE', '运费计费模式')
on conflict (dict_code) do nothing;

insert into t_dict_items (dict_type_id, item_code, item_label, sort_no)
select dt.id, x.item_code, x.item_label, x.sort_no
from t_dict_types dt
join (
  values
    ('ORDER_STATUS', 'PENDING_PAYMENT', '待支付', 10),
    ('ORDER_STATUS', 'PAID', '已支付', 20),
    ('ORDER_STATUS', 'SHIPPED', '已发货', 30),
    ('ORDER_STATUS', 'DELIVERED', '已送达', 40),
    ('ORDER_STATUS', 'COMPLETED', '已完成', 50),
    ('AFTER_SALE_STATUS', 'OPEN', '待处理', 10),
    ('AFTER_SALE_STATUS', 'PROCESSING', '处理中', 20),
    ('AFTER_SALE_STATUS', 'RESOLVED', '已完结', 30),
    ('COOP_STATUS', 'NEW', '新线索', 10),
    ('COOP_STATUS', 'FOLLOWING', '跟进中', 20),
    ('COOP_STATUS', 'CLOSED', '已关闭', 30),
    ('SHIPPING_BILLING_MODE', 'BY_WEIGHT', '按重量', 10),
    ('SHIPPING_BILLING_MODE', 'FLAT', '固定运费', 20)
) as x(dict_code, item_code, item_label, sort_no)
on dt.dict_code = x.dict_code
on conflict (dict_type_id, item_code) do nothing;

create index if not exists idx_t_products_category_id on t_products(category_id);
create index if not exists idx_t_products_shipping_template_id on t_products(shipping_template_id);
create index if not exists idx_t_product_skus_product_id on t_product_skus(product_id);
create index if not exists idx_t_user_addresses_user_id on t_user_addresses(user_id);
create index if not exists idx_t_browse_histories_user_id on t_browse_histories(user_id);
create index if not exists idx_t_order_status_histories_order_id on t_order_status_histories(order_id);
