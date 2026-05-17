-- 分类支持中英文名称，前台按语言切换展示
alter table t_categories add column if not exists name_zh varchar(120);
alter table t_categories add column if not exists name_en varchar(120);

update t_categories
set
  name_zh = coalesce(nullif(trim(name_zh), ''), trim(name)),
  name_en = coalesce(nullif(trim(name_en), ''), trim(name))
where name is not null;

alter table t_categories alter column name_zh set not null;
alter table t_categories alter column name_en set not null;

alter table t_categories drop column if exists name;
