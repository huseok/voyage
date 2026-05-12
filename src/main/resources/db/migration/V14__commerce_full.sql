-- 购物车：行级「是否参与结算」勾选
alter table t_cart_items add column if not exists selected boolean not null default true;

-- 用户收货地址：结构化字段（公司 / 省 / 市），历史数据可为空
alter table t_user_addresses add column if not exists receiver_company varchar(120);
alter table t_user_addresses add column if not exists province varchar(100);
alter table t_user_addresses add column if not exists city varchar(100);

-- 订单：结构化收货、支付与金额拆分快照（下单时固化，避免后续改价影响已生成订单）
alter table t_orders add column if not exists receiver_province varchar(100);
alter table t_orders add column if not exists receiver_city varchar(100);
alter table t_orders add column if not exists payment_status varchar(32) not null default 'UNPAID';
alter table t_orders add column if not exists subtotal_amount numeric(12,2);
alter table t_orders add column if not exists discount_member numeric(12,2) not null default 0;
alter table t_orders add column if not exists discount_coupon numeric(12,2) not null default 0;
alter table t_orders add column if not exists discount_promo numeric(12,2) not null default 0;
alter table t_orders add column if not exists shipping_fee numeric(12,2) not null default 0;
alter table t_orders add column if not exists coupon_code_snapshot varchar(64);
alter table t_orders add column if not exists paypal_order_id varchar(128);

-- 历史订单：拆分金额默认等于原总额（无券/无运费细分的旧单）
update t_orders set subtotal_amount = total_amount where subtotal_amount is null;

alter table t_orders alter column subtotal_amount set default 0;
alter table t_orders alter column subtotal_amount set not null;

-- 订单行：商品主图快照（列表/详情展示）
alter table t_order_items add column if not exists thumb_url varchar(512);

-- 会员累计（按用户维度一条记录）
create table if not exists t_user_membership (
  user_id bigint primary key references t_users(id),
  lifetime_paid_usd numeric(14,2) not null default 0,
  tier varchar(20) not null default 'NONE',
  updated_at timestamptz not null default now()
);

-- 优惠券（后台维护，前台下单传 code）
create table if not exists t_coupons (
  id bigserial primary key,
  code varchar(64) not null unique,
  name varchar(200) not null,
  discount_type varchar(16) not null,
  discount_value numeric(12,2) not null,
  min_order_amount numeric(12,2) not null default 0,
  tag_filter varchar(400),
  valid_from timestamptz not null,
  valid_to timestamptz not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 满减活动（满足门槛减固定金额；可与券叠加策略在应用层控制）
create table if not exists t_promotions (
  id bigserial primary key,
  name varchar(200) not null,
  threshold_amount numeric(12,2) not null,
  amount_off numeric(12,2) not null,
  tag_filter varchar(400),
  valid_from timestamptz not null,
  valid_to timestamptz not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
