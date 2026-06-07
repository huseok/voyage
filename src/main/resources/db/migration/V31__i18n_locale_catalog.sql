-- 前台/后台 UI 文案目录（按 locale 存整份 JSON）
CREATE TABLE t_i18n_locale_catalog (
    locale      VARCHAR(16) PRIMARY KEY,
    content     JSONB NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_i18n_locale_catalog_updated ON t_i18n_locale_catalog (updated_at DESC);
