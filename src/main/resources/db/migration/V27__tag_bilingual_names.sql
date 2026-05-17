-- 标签支持中英文名称，前台按语言切换展示
ALTER TABLE t_tags ADD COLUMN IF NOT EXISTS name_zh VARCHAR(120);
ALTER TABLE t_tags ADD COLUMN IF NOT EXISTS name_en VARCHAR(120);

UPDATE t_tags
SET
  name_zh = COALESCE(NULLIF(TRIM(name_zh), ''), TRIM(name)),
  name_en = COALESCE(NULLIF(TRIM(name_en), ''), TRIM(name))
WHERE name IS NOT NULL;

UPDATE t_tags SET name_zh = '' WHERE name_zh IS NULL;
UPDATE t_tags SET name_en = '' WHERE name_en IS NULL;

ALTER TABLE t_tags ALTER COLUMN name_zh SET NOT NULL;
ALTER TABLE t_tags ALTER COLUMN name_en SET NOT NULL;

ALTER TABLE t_tags DROP COLUMN IF EXISTS name;
