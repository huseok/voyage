-- 汇率全局配置（单行 id=1）
CREATE TABLE t_exchange_rate_settings (
    id              BIGINT PRIMARY KEY,
    base_currency   VARCHAR(8) NOT NULL DEFAULT 'USD',
    refresh_interval_hours INT NOT NULL DEFAULT 12,
    default_markup_percent NUMERIC(8, 4) NOT NULL DEFAULT 0,
    provider_url    VARCHAR(512) NOT NULL DEFAULT 'https://api.frankfurter.app/latest',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO t_exchange_rate_settings (id, base_currency, refresh_interval_hours, default_markup_percent)
VALUES (1, 'USD', 12, 0);

-- 各目标币种汇率：1 USD = market_rate 单位目标币
CREATE TABLE t_exchange_rates (
    id                      BIGSERIAL PRIMARY KEY,
    currency_code           VARCHAR(8) NOT NULL,
    market_rate             NUMERIC(18, 8) NOT NULL DEFAULT 1,
    effective_rate          NUMERIC(18, 8) NOT NULL DEFAULT 1,
    markup_percent          NUMERIC(8, 4) NOT NULL DEFAULT 0,
    markup_amount           NUMERIC(18, 8) NOT NULL DEFAULT 0,
    frozen_rate             NUMERIC(18, 8),
    freeze_until            TIMESTAMPTZ,
    refresh_interval_hours  INT,
    last_fetched_at         TIMESTAMPTZ,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_exchange_rates_currency UNIQUE (currency_code)
);

CREATE INDEX idx_exchange_rates_enabled ON t_exchange_rates (enabled);

-- 初始占位汇率（定时任务拉取后会更新）
INSERT INTO t_exchange_rates (currency_code, market_rate, effective_rate, enabled) VALUES
    ('USD', 1, 1, TRUE),
    ('CNY', 7.24, 7.24, TRUE),
    ('EUR', 0.92, 0.92, TRUE),
    ('GBP', 0.79, 0.79, TRUE),
    ('JPY', 150.0, 150.0, TRUE),
    ('KRW', 1350.0, 1350.0, TRUE),
    ('RUB', 92.0, 92.0, TRUE),
    ('BRL', 5.0, 5.0, TRUE),
    ('SAR', 3.75, 3.75, TRUE),
    ('TRY', 32.0, 32.0, TRUE),
    ('PLN', 4.0, 4.0, TRUE),
    ('VND', 24500.0, 24500.0, TRUE),
    ('THB', 36.0, 36.0, TRUE),
    ('IDR', 15800.0, 15800.0, TRUE),
    ('INR', 83.0, 83.0, TRUE);
