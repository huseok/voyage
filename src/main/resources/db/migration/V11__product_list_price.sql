-- 商品划线原价（可选）：用于首页「活动商品」等展示原价 vs 现价；NULL 表示无促销划线价。
alter table t_products
  add column if not exists list_price numeric(12, 2);

comment on column t_products.list_price is '划线原价；非空且大于 price 时视为活动价展示';

-- 本地种子商品补充示例划线价（幂等更新）
update t_products set list_price = 39.90 where sku_code = 'SKU-EBP-001' and (list_price is null or list_price = price);
update t_products set list_price = 24.90 where sku_code = 'SKU-LAMP-002' and (list_price is null or list_price = price);
update t_products set list_price = 28.00 where sku_code = 'SKU-PB-003' and (list_price is null or list_price = price);
