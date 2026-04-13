# CHZfobkey 后端架构说明

## 1. 项目定位

- 单一后端项目：`voyage`
- 技术栈：Spring Boot + Kotlin + JPA + Flyway + PostgreSQL
- 统一前缀：
  - 前台 API：`/api/v1/**`
  - 后台 API：`/api/v1/admin/**`

## 2. 领域模块

- `auth`：登录注册、令牌刷新、当前用户
- `product`：商品查询与后台维护（含重量、分类、运费模板字段）
- `category`：分类树维护
- `shipping`：运费模板与规则
- `cart`：购物车
- `order`：下单、订单状态流转
- `aftersale`：售后
- `content`：站点内容与商业合作线索
- `usercenter`：地址与浏览历史
- `dictionary`：字典类型/字典项
- `audit`：操作日志与订单状态历史

## 3. 数据库迁移

- 基线：`V1__init_schema.sql`
- 新增扩展：`V4__domain_dictionary_audit_upgrade.sql`
- 迁移原则：
  - 只做增量，不做破坏式删改
  - 旧字段保兼容，逐步演进

## 4. 状态与审计

- 订单状态流转会写入 `t_order_status_histories`
- 关键操作可写入 `t_audit_logs`
- 字典表统一维护状态展示与配置项

## 5. 如何扩展

- 新业务优先新增独立包：`controller + service + entity + repository + dto`
- 接口改动后同步 OpenAPI 文档，供前端类型生成
- 涉及结构变更必须新增 Flyway 版本脚本
