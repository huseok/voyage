-- 订单物流轨迹：与订单号关联，支持多条历史（单独维护，不替代订单主表上的最新运单字段）
CREATE TABLE IF NOT EXISTS t_order_logistics (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL REFERENCES t_orders (order_no) ON DELETE CASCADE,
    carrier VARCHAR(120),
    tracking_no VARCHAR(128) NOT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_t_order_logistics_order_no ON t_order_logistics (order_no);
