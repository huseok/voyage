-- V1 初始化脚本（PostgreSQL）
-- 说明：保留核心业务与外贸常用字段，后续通过新版本迁移持续演进。

create table if not exists t_users (
  id bigserial primary key,
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  name varchar(100) not null,
  phone varchar(30),
  country varchar(60),
  role varchar(30) not null default 'CUSTOMER',
  status varchar(20) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_products (
  id bigserial primary key,
  title varchar(255) not null,
  price numeric(12,2) not null,
  currency varchar(8) not null default 'USD',
  moq int not null default 1,
  description text,
  sku_code varchar(64),
  hs_code varchar(32),
  unit varchar(20),
  incoterm varchar(20),
  origin_country varchar(60),
  lead_time_days int,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_cart_items (
  id bigserial primary key,
  user_id bigint not null references t_users(id),
  product_id bigint not null references t_products(id),
  quantity int not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_t_cart_items_user_product unique (user_id, product_id)
);

create table if not exists t_orders (
  id bigserial primary key,
  order_no varchar(64) not null unique,
  user_id bigint not null references t_users(id),
  status varchar(32) not null,
  total_amount numeric(12,2) not null,
  currency varchar(8) not null default 'USD',
  receiver_name varchar(100) not null,
  receiver_phone varchar(30) not null,
  receiver_company varchar(120),
  tax_no varchar(60),
  country varchar(60) not null,
  address_line varchar(255) not null,
  postal_code varchar(20),
  incoterm varchar(20),
  shipping_method varchar(30),
  logistics_company varchar(100),
  tracking_no varchar(100),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_order_items (
  id bigserial primary key,
  order_id bigint not null references t_orders(id),
  product_id bigint not null references t_products(id),
  title_snapshot varchar(255) not null,
  price_snapshot numeric(12,2) not null,
  quantity int not null,
  created_at timestamptz not null default now()
);

create table if not exists t_after_sales (
  id bigserial primary key,
  user_id bigint not null references t_users(id),
  order_no varchar(64) not null,
  status varchar(32) not null default 'OPEN',
  content text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_t_orders_user_id on t_orders(user_id);
create index if not exists idx_t_cart_items_user_id on t_cart_items(user_id);
create index if not exists idx_t_after_sales_user_id on t_after_sales(user_id);
