-- 商品成本价（可选）：仅管理端维护与返回，前台接口不露价。
alter table t_products
  add column if not exists cost_price numeric(12, 2);

comment on column t_products.cost_price is '成本价；可选；前台 ProductView 固定为 null';
