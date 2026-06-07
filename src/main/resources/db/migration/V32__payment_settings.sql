-- PayPal 等支付全局配置（单行 id=1）
CREATE TABLE t_payment_settings (
    id              BIGINT PRIMARY KEY DEFAULT 1,
    provider        VARCHAR(32) NOT NULL DEFAULT 'paypal',
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    sandbox         BOOLEAN NOT NULL DEFAULT TRUE,
    client_id       VARCHAR(256),
    client_secret   VARCHAR(512),
    webhook_id      VARCHAR(128),
    brand_name      VARCHAR(128) NOT NULL DEFAULT 'Globuy',
    return_url_base VARCHAR(512),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO t_payment_settings (id) VALUES (1);
