-- SKU 规格矩阵：属性维度名与取值走字典，供后台规格矩阵页选用（可在「字典管理」继续扩充）
insert into t_dict_types (dict_code, dict_name)
values
  ('PRODUCT_SKU_ATTR', '商品 SKU 属性维度'),
  ('PRODUCT_SKU_ATTR_VALUE', '商品 SKU 属性取值')
on conflict (dict_code) do nothing;

insert into t_dict_items (dict_type_id, item_code, item_label, sort_no)
select dt.id, x.item_code, x.item_label, x.sort_no
from t_dict_types dt
join (
  values
    ('PRODUCT_SKU_ATTR', 'COLOR', 'Color', 10),
    ('PRODUCT_SKU_ATTR', 'SIZE', 'Size', 20),
    ('PRODUCT_SKU_ATTR', 'PACK', 'Pack', 30),
    ('PRODUCT_SKU_ATTR', 'MATERIAL', 'Material', 40),
    ('PRODUCT_SKU_ATTR_VALUE', 'COLOR:BLACK', 'Black', 10),
    ('PRODUCT_SKU_ATTR_VALUE', 'COLOR:WHITE', 'White', 20),
    ('PRODUCT_SKU_ATTR_VALUE', 'COLOR:RED', 'Red', 30),
    ('PRODUCT_SKU_ATTR_VALUE', 'SIZE:XS', 'XS', 10),
    ('PRODUCT_SKU_ATTR_VALUE', 'SIZE:S', 'S', 20),
    ('PRODUCT_SKU_ATTR_VALUE', 'SIZE:M', 'M', 30),
    ('PRODUCT_SKU_ATTR_VALUE', 'SIZE:L', 'L', 40),
    ('PRODUCT_SKU_ATTR_VALUE', 'SIZE:XL', 'XL', 50),
    ('PRODUCT_SKU_ATTR_VALUE', 'PACK:1PC', '1pc', 10),
    ('PRODUCT_SKU_ATTR_VALUE', 'PACK:2PC', '2pc', 20),
    ('PRODUCT_SKU_ATTR_VALUE', 'PACK:6PC', '6pc', 30),
    ('PRODUCT_SKU_ATTR_VALUE', 'MATERIAL:PLASTIC', 'Plastic', 10),
    ('PRODUCT_SKU_ATTR_VALUE', 'MATERIAL:METAL', 'Metal', 20),
    ('PRODUCT_SKU_ATTR_VALUE', 'MATERIAL:WOOD', 'Wood', 30)
) as x(dict_code, item_code, item_label, sort_no)
on dt.dict_code = x.dict_code
on conflict (dict_type_id, item_code) do nothing;
