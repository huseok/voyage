# 数据字典与中文对照（Voyage）

## 统一审计字段（所有业务表）

- `created_by`：创建人（用户ID）
- `created_at`：创建时间
- `updated_by`：修改人（用户ID）
- `updated_at`：修改时间
- `is_deleted`：逻辑删除标记（`false`=有效，`true`=已删除）
- `deleted_by`：删除人（用户ID）
- `deleted_at`：删除时间

## 业务状态字典（来自 `t_dict_types` / `t_dict_items`）

### `ORDER_STATUS` 订单状态

- `PENDING_PAYMENT`：待支付
- `PAID`：已支付
- `SHIPPED`：已发货
- `DELIVERED`：已送达
- `COMPLETED`：已完成

### `AFTER_SALE_STATUS` 售后状态

- `OPEN`：待处理
- `PROCESSING`：处理中
- `RESOLVED`：已完结

### `COOP_STATUS` 合作线索状态

- `NEW`：新线索
- `FOLLOWING`：跟进中
- `CLOSED`：已关闭

### `SHIPPING_BILLING_MODE` 运费计费模式

- `BY_WEIGHT`：按重量
- `FLAT`：固定运费

## 表中文说明（核心）

- `t_users`：用户表
- `t_products`：商品主表
- `t_product_skus`：商品 SKU 表（规格组合后的最小售卖单元）
- `t_product_options`：商品规格选项表
- `t_cart_items`：购物车项
- `t_orders`：订单主表
- `t_order_items`：订单明细
- `t_after_sales`：售后工单
- `t_categories`：分类树
- `t_shipping_templates`：运费模板
- `t_shipping_template_rules`：运费模板规则
- `t_user_addresses`：用户地址簿
- `t_browse_histories`：浏览记录
- `t_site_contents`：站点内容（CMS）
- `t_business_cooperations`：商务合作线索
- `t_dict_types`：字典类型
- `t_dict_items`：字典项
- `t_audit_logs`：操作审计日志
- `t_order_status_histories`：订单状态流转历史
- `t_price_change_histories`：价格变更历史

## 你最关心的“对应中文怎么查”

- 字段和状态值一律看本文件；
- 实际数据以字典表为准：
  - 类型表：`t_dict_types.dict_code`
  - 项表：`t_dict_items.item_code / item_label`
- 运营后台显示建议直接用 `item_label`（中文）渲染。
