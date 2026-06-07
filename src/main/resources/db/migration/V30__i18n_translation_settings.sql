-- 机器翻译全局配置（单行 id=1）；API Key 可在后台维护，环境变量仍可覆盖
CREATE TABLE t_i18n_translation_settings (
    id                      BIGINT PRIMARY KEY,
    provider                VARCHAR(32) NOT NULL DEFAULT 'deepl',
    api_key                 VARCHAR(512),
    api_url                 VARCHAR(512) NOT NULL DEFAULT 'https://api-free.deepl.com',
    default_source_locale   VARCHAR(16) NOT NULL DEFAULT 'en-US',
    max_texts_per_request   INT NOT NULL DEFAULT 50,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO t_i18n_translation_settings (id, provider, api_url, default_source_locale, max_texts_per_request, enabled)
VALUES (1, 'deepl', 'https://api-free.deepl.com', 'en-US', 50, TRUE);
