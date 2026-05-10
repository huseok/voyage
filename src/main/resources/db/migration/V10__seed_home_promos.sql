-- 首页活动轮播示例（content_type = PROMO）；后台「站点内容」可改 key / 配图 / 文案 / 跳转
INSERT INTO t_site_contents (content_key, content_type, title, subtitle, body, image_url, action_url, sort_no, is_active, created_at, updated_at, is_deleted)
VALUES
  (
    'HOME_PROMO_1',
    'PROMO',
    '春季采购周',
    '多国仓储 · 极速履约',
    '精选 SKU 让利促销，登录后可查看合约价与库存；支持试单与小批量起订。',
    NULL,
    '/catalog',
    0,
    true,
    now(),
    now(),
    false
  ),
  (
    'HOME_PROMO_2',
    'PROMO',
    '类目直达',
    '美妆 · 个护 · 家居',
    '按分类浏览热卖商品，组合下单更省心。',
    NULL,
    '/catalog',
    1,
    true,
    now(),
    now(),
    false
  )
ON CONFLICT (content_key) DO NOTHING;
