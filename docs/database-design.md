# 数据库设计说明

本文档描述 EveryCent 后续业务数据库设计方向。当前 JHipster 已生成用户和权限相关表，业务表应通过新增 Liquibase changelog 逐步加入。

## 核心实体

### User

用户实体，复用 JHipster 已生成的 `jhi_user` 表和权限体系。用于登录、认证、审计和账本归属。

### Ledger

账本实体，用于区分个人账本和共享账本。

建议字段：

- `id`
- `name`
- `description`
- `owner_id`
- `created_date`
- `last_modified_date`

### LedgerPermission 或 UserLedgerAuth

用户账本权限实体，用于支持多人共同记账和只读监督。

建议字段：

- `id`
- `ledger_id`
- `user_id`
- `permission_type`：`OWNER`、`WRITE`、`READ`
- `created_date`

### Transaction

收支记录实体，用于记录收入、支出、行为标签和情绪标签。

建议字段：

- `id`
- `ledger_id`
- `amount`
- `type`：`INCOME` 或 `EXPENSE`
- `behavior_tag`
- `mood_tag`
- `occurred_date`
- `remark`
- `created_by`
- `created_date`

### Budget

预算实体，用于维护周预算和月预算。

建议字段：

- `id`
- `ledger_id`
- `period_type`：`WEEK` 或 `MONTH`
- `period_value`
- `amount`
- `created_date`

## 实体关系

- 一个 `User` 可以拥有多个 `Ledger`。
- 一个 `Ledger` 只能有一个所有者，但可以通过 `LedgerPermission` 授权多个用户访问。
- 一个 `Ledger` 可以包含多条 `Transaction`。
- 一个 `Ledger` 可以配置多个 `Budget`，按周或按月区分。
- `Transaction.created_by` 用于记录多人账本中是哪位用户录入。

## 3NF 设计原则

- 每张表只描述一个业务对象，避免把用户、账本、预算、记录混在一张表。
- 非主键字段应直接依赖主键，不通过其他非主键字段间接依赖。
- 行为标签、情绪标签和权限类型建议使用枚举或受控字典，避免自由文本导致统计困难。
- 汇总数据如月余额、分类占比优先通过查询计算；如需缓存汇总结果，应明确刷新规则。

## Liquibase 管理规范

- 所有业务表结构变更都应新增 `src/main/resources/config/liquibase/changelog/` 下的 changelog。
- 新 changelog 应在 `src/main/resources/config/liquibase/master.xml` 中 include。
- 已经合并并被团队成员使用过的 changelog 不要直接修改，应通过新的 changelog 增量变更。
- changelog 文件命名建议包含时间戳和业务含义，例如 `20260615000100_add_ledger_tables.xml`。

## 触发器、完整性约束和测试数据

- 外键、唯一约束、非空约束应优先在 Liquibase changelog 中声明。
- 预算超支、余额计算等业务逻辑优先放在后端服务层，只有确有必要时才使用数据库触发器。
- 如需触发器，也应通过 Liquibase 管理，禁止手动在本地数据库直接创建后不提交。
- 测试数据可放在 Liquibase data CSV 或测试专用 changelog 中，避免污染生产配置。
- 涉及金额的字段应使用精确小数类型，避免使用浮点类型。
