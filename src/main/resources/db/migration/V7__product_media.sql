-- 商品图片：缩略图 + 「完整」图（完整图尺寸由应用配置决定，可为原图或压缩大图）
create table if not exists t_product_media (
  id bigserial primary key,
  product_id bigint not null references t_products(id) on delete cascade,
  thumb_url varchar(512) not null,
  full_url varchar(512) not null,
  sort_no int not null default 0,
  created_at timestamptz not null default now()
);

create index if not exists idx_t_product_media_product on t_product_media(product_id);

comment on table t_product_media is '商品多媒体：前台列表默认展示 thumb_url；详情可加载 full_url';
comment on column t_product_media.thumb_url is '相对站点根的 URL 路径，如 /media/2026/05/uuid_thumb.jpg';
comment on column t_product_media.full_url is '相对站点根的 URL 路径，如 /media/2026/05/uuid_full.jpg';
