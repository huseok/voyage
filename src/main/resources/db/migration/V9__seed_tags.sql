-- 示例标签（可在后台「标签管理」中增删改）
insert into t_tags (code, name, sort_no, is_active)
values
  ('HOT', '热卖', 10, true),
  ('NEW', '新品', 20, true),
  ('EXPORT', '出口友好', 30, true)
on conflict (code) do nothing;
