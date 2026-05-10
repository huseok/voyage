-- 首页「活动商品」展示区：由标签编码 HOME_PROMO 控制；后台给商品勾选该标签即可出现在该区域。
insert into t_tags (code, name, sort_no, is_active)
values ('HOME_PROMO', '首页活动商品', 5, true)
on conflict (code) do nothing;

insert into t_product_tags (product_id, tag_id, created_at)
select p.id, t.id, now()
from t_products p
cross join t_tags t
where t.code = 'HOME_PROMO'
  and p.sku_code in ('SKU-EBP-001', 'SKU-LAMP-002', 'SKU-PB-003')
on conflict on constraint uk_t_product_tags_product_tag do nothing;
