-- V3 测试数据脚本（本地/测试环境）
-- 说明：
-- 1) 表名统一使用小写 + 下划线（snake_case）命名；
-- 2) 尽量幂等，重复执行不会产生明显脏数据。

-- 测试用户（密码明文均为 Admin@123456，与 V2 同一 BCrypt）
insert into t_users (email, password_hash, name, phone, country, role, status)
values
  (
    'buyer1@voyage.local',
    '$2b$10$kXRYfWNApj0moXpo3.Wd1Oso.HZ6.VTWo7NVZii6i1Ag87wLK.cQG',
    'Buyer One',
    '+86-13800000001',
    'CN',
    'CUSTOMER',
    'ACTIVE'
  ),
  (
    'buyer2@voyage.local',
    '$2b$10$kXRYfWNApj0moXpo3.Wd1Oso.HZ6.VTWo7NVZii6i1Ag87wLK.cQG',
    'Buyer Two',
    '+1-202-555-0102',
    'US',
    'CUSTOMER',
    'ACTIVE'
  )
on conflict (email) do nothing;

-- 测试商品
insert into t_products (
  title, price, currency, moq, description, sku_code, hs_code, unit, incoterm, origin_country, lead_time_days, is_active
)
values
  (
    'Wireless Earbuds Pro',
    29.90,
    'USD',
    10,
    'Bluetooth 5.3 earbuds with ENC noise reduction.',
    'SKU-EBP-001',
    '851830',
    'pair',
    'FOB',
    'CN',
    7,
    true
  ),
  (
    'Smart LED Desk Lamp',
    18.50,
    'USD',
    20,
    'Dimmable LED desk lamp with USB-C charging port.',
    'SKU-LAMP-002',
    '940520',
    'pcs',
    'EXW',
    'CN',
    10,
    true
  ),
  (
    'Portable Power Bank 20000mAh',
    22.00,
    'USD',
    15,
    'Fast-charging power bank with dual output.',
    'SKU-PB-003',
    '850760',
    'pcs',
    'FOB',
    'CN',
    8,
    true
  )
on conflict do nothing;

-- 测试购物车（buyer1）
insert into t_cart_items (user_id, product_id, quantity)
select u.id, p.id, 2
from t_users u
join t_products p on p.sku_code = 'SKU-EBP-001'
where u.email = 'buyer1@voyage.local'
on conflict (user_id, product_id) do update
set quantity = excluded.quantity, updated_at = now();

insert into t_cart_items (user_id, product_id, quantity)
select u.id, p.id, 1
from t_users u
join t_products p on p.sku_code = 'SKU-LAMP-002'
where u.email = 'buyer1@voyage.local'
on conflict (user_id, product_id) do update
set quantity = excluded.quantity, updated_at = now();

-- 测试订单（buyer2）
insert into t_orders (
  order_no, user_id, status, total_amount, currency,
  receiver_name, receiver_phone, receiver_company, tax_no, country, address_line, postal_code,
  incoterm, shipping_method, logistics_company, tracking_no
)
select
  'VOY-TEST-20260408-0001',
  u.id,
  'PAID',
  62.50,
  'USD',
  'Buyer Two',
  '+1-202-555-0102',
  'Voyage Buyer LLC',
  'US-TAX-778899',
  'US',
  '800 Market Street, San Francisco, CA',
  '94103',
  'FOB',
  'SEA',
  'Maersk',
  'TRK-VOY-TEST-0001'
from t_users u
where u.email = 'buyer2@voyage.local'
on conflict (order_no) do nothing;

-- 测试订单明细
insert into t_order_items (order_id, product_id, title_snapshot, price_snapshot, quantity)
select
  o.id,
  p.id,
  p.title,
  p.price,
  1
from t_orders o
join t_products p on p.sku_code = 'SKU-EBP-001'
where o.order_no = 'VOY-TEST-20260408-0001'
  and not exists (
    select 1
    from t_order_items oi
    where oi.order_id = o.id
      and oi.product_id = p.id
  );

insert into t_order_items (order_id, product_id, title_snapshot, price_snapshot, quantity)
select
  o.id,
  p.id,
  p.title,
  p.price,
  1
from t_orders o
join t_products p on p.sku_code = 'SKU-PB-003'
where o.order_no = 'VOY-TEST-20260408-0001'
  and not exists (
    select 1
    from t_order_items oi
    where oi.order_id = o.id
      and oi.product_id = p.id
  );

-- 测试售后单
insert into t_after_sales (user_id, order_no, status, content)
select
  u.id,
  'VOY-TEST-20260408-0001',
  'OPEN',
  'Requesting replacement due to packaging damage.'
from t_users u
where u.email = 'buyer2@voyage.local'
  and not exists (
    select 1
    from t_after_sales a
    where a.user_id = u.id
      and a.order_no = 'VOY-TEST-20260408-0001'
      and a.content = 'Requesting replacement due to packaging damage.'
  );
