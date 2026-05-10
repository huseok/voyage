-- 商品标签：主表 + 与商品的关联表（多对多经由 t_product_tags）

create table if not exists t_tags (
  id bigserial primary key,
  code varchar(64) not null unique,
  name varchar(120) not null,
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists t_product_tags (
  id bigserial primary key,
  product_id bigint not null references t_products(id) on delete cascade,
  tag_id bigint not null references t_tags(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint uk_t_product_tags_product_tag unique (product_id, tag_id)
);

create index if not exists idx_t_product_tags_product_id on t_product_tags(product_id);
create index if not exists idx_t_product_tags_tag_id on t_product_tags(tag_id);
