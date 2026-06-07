-- 主数据多语言：统一 i18n JSONB，并从既有中英列回填

ALTER TABLE t_categories
    ADD COLUMN IF NOT EXISTS i18n JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE t_categories
SET i18n = jsonb_strip_nulls(
    jsonb_build_object(
        'zh-CN', NULLIF(trim(name_zh), ''),
        'en-US', NULLIF(trim(name_en), '')
    )
)
WHERE i18n = '{}'::jsonb OR i18n IS NULL;

ALTER TABLE t_tags
    ADD COLUMN IF NOT EXISTS i18n JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE t_tags
SET i18n = jsonb_strip_nulls(
    jsonb_build_object(
        'zh-CN', NULLIF(trim(name_zh), ''),
        'en-US', NULLIF(trim(name_en), '')
    )
)
WHERE i18n = '{}'::jsonb OR i18n IS NULL;

ALTER TABLE t_products
    ADD COLUMN IF NOT EXISTS i18n JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE t_products
SET i18n = jsonb_strip_nulls(
    jsonb_build_object(
        'en-US', jsonb_strip_nulls(
            jsonb_build_object(
                'title', NULLIF(trim(title), ''),
                'description', NULLIF(trim(description), '')
            )
        ),
        'zh-CN', jsonb_strip_nulls(
            jsonb_build_object(
                'title', NULLIF(trim(title), ''),
                'description', NULLIF(trim(description), '')
            )
        )
    )
)
WHERE i18n = '{}'::jsonb OR i18n IS NULL;

ALTER TABLE t_site_contents
    ADD COLUMN IF NOT EXISTS i18n JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE t_site_contents
SET i18n = jsonb_strip_nulls(
    jsonb_build_object(
        'en-US', jsonb_strip_nulls(
            jsonb_build_object(
                'title', NULLIF(trim(title), ''),
                'subtitle', NULLIF(trim(subtitle), ''),
                'body', NULLIF(trim(body), '')
            )
        ),
        'zh-CN', jsonb_strip_nulls(
            jsonb_build_object(
                'title', NULLIF(trim(title), ''),
                'subtitle', NULLIF(trim(subtitle), ''),
                'body', NULLIF(trim(body), '')
            )
        )
    )
)
WHERE i18n = '{}'::jsonb OR i18n IS NULL;

ALTER TABLE t_sku_attr_dimensions
    ADD COLUMN IF NOT EXISTS i18n JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE t_sku_attr_dimensions
SET i18n = jsonb_strip_nulls(
    jsonb_build_object(
        'en-US', NULLIF(trim(name), ''),
        'zh-CN', NULLIF(trim(name), '')
    )
)
WHERE i18n = '{}'::jsonb OR i18n IS NULL;

ALTER TABLE t_sku_attr_values
    ADD COLUMN IF NOT EXISTS i18n JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE t_sku_attr_values
SET i18n = jsonb_strip_nulls(
    jsonb_build_object(
        'en-US', NULLIF(trim(name), ''),
        'zh-CN', NULLIF(trim(name), '')
    )
)
WHERE i18n = '{}'::jsonb OR i18n IS NULL;
