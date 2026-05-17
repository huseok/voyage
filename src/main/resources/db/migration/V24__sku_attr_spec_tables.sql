-- SKU 规格属性独立表（不再依赖字典 PRODUCT_SKU_ATTR / PRODUCT_SKU_ATTR_VALUE）
create table if not exists t_sku_attr_dimensions (
  id bigserial primary key,
  code varchar(64) not null,
  name varchar(120) not null,
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_sku_attr_dimensions_code unique (code)
);

create table if not exists t_sku_attr_values (
  id bigserial primary key,
  dimension_id bigint not null references t_sku_attr_dimensions(id) on delete cascade,
  code varchar(64) not null,
  name varchar(120) not null,
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_sku_attr_values_dim_code unique (dimension_id, code)
);

create index if not exists idx_sku_attr_values_dimension on t_sku_attr_values (dimension_id);

-- 从字典种子迁移（历史 item_code 可能为 COLOR:BLACK，拆成维度 + 取值两列）
insert into t_sku_attr_dimensions (code, name, sort_no, is_active)
select distinct on (upper(di.item_code))
  upper(di.item_code),
  di.item_label,
  di.sort_no,
  di.is_active
from t_dict_items di
join t_dict_types dt on dt.id = di.dict_type_id and dt.dict_code = 'PRODUCT_SKU_ATTR'
where di.is_deleted = false
order by upper(di.item_code), di.id
on conflict (code) do nothing;

insert into t_sku_attr_values (dimension_id, code, name, sort_no, is_active)
select
  d.id,
  upper(split_part(di.item_code, ':', 2)),
  di.item_label,
  di.sort_no,
  di.is_active
from t_dict_items di
join t_dict_types dt on dt.id = di.dict_type_id and dt.dict_code = 'PRODUCT_SKU_ATTR_VALUE'
join t_sku_attr_dimensions d on d.code = upper(split_part(di.item_code, ':', 1))
where di.is_deleted = false
  and position(':' in di.item_code) > 0
on conflict (dimension_id, code) do nothing;
