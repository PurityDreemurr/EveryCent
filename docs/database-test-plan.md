# EveryCent 数据库测试计划

本计划按系统详细设计文档中的表结构设计。演示数据建议用于测试环境，不写入正式初始化 changelog。

## 完整性约束测试

- 插入 `ledger` 时缺少 `name`、`creator_id`、`default_currency`、`created_date` 应失败。
- 插入 `user_ledger_permission` 时缺少 `user_id`、`ledger_id`、`permission_level`、`status`、`created_date` 应失败。
- 插入 `transaction_record` 时缺少 `ledger_id`、`creator_id`、`amount`、`type`、`transaction_date`、`source`、`created_date` 应失败。
- 插入 `budget` 时缺少 `ledger_id`、`cycle`、`period_start`、`period_end`、`limit_amount`、`alert_threshold`、`enabled` 应失败。
- 插入 `monthly_balance` 时缺少 `ledger_id`、`year`、`month`、`updated_date` 应失败。

## 外键约束测试

- 使用不存在的 `jhi_user.id` 创建 `ledger.creator_id` 应失败。
- 使用不存在的账本或用户创建 `user_ledger_permission` 应失败。
- 使用不存在的标签创建 `transaction_record.behavior_tag_id` 或 `emotion_tag_id` 应失败。
- 使用不存在的账本创建 `budget`、`monthly_balance`、`notification_message.ledger_id` 应失败。
- 使用不存在的预算创建 `notification_message.budget_id` 应失败。

## 唯一约束测试

- 同一 `user_id + ledger_id` 重复插入 `user_ledger_permission` 应失败。
- 同一 `ledger_id + cycle + period_start + period_end` 重复插入 `budget` 应失败。
- 同一 `ledger_id + year + month` 重复插入 `monthly_balance` 应失败。
- 重复插入同一 `behavior_tag.code` 或 `emotion_tag.code` 应失败。

## 权限测试

- `OWNER` 可查看账本、写记录、设置预算、邀请成员、删除账本。
- `READ_WRITE` 可查看账本、写记录、设置预算，但不能邀请成员或删除账本。
- `READ_ONLY` 只能查看账本、记录、预算，不能新增或修改记录。
- `PENDING` 或 `REVOKED` 状态不应获得账本访问权限。

## 预算超支测试

- 创建 `MONTHLY` 预算，`limit_amount = 1000`，`alert_threshold = 0.80`。
- 插入周期内支出 700，超阈值查询不返回。
- 再插入周期内支出 150，超阈值查询返回。
- 插入周期外支出，不应影响该预算使用比例。

## 月度余额计算测试

- 同账本同月插入收入 5000、支出 1200，重算后 `total_income = 5000`、`total_expense = 1200`、`balance = 3800`。
- 修改收支记录金额后，Service 层应重算对应月份。
- 修改 `transaction_date` 跨月份时，应重算旧月份和新月份。
- 删除记录后，应重算对应月份并同步 `ledger.current_month_balance`。

## 测试数据建议

- 个人账本：`My Daily Ledger`，用户 A 为 `OWNER`。
- 家庭监督账本：用户 A 为 `OWNER`，用户 B 为 `READ_ONLY`。
- 情侣共同账本：用户 A 为 `OWNER`，用户 B 为 `READ_WRITE`。
- 收入记录：`SALARY/HAPPY`、`PART_TIME/CALM`。
- 支出记录：`FOOD/NONE`、`SHOPPING/IMPULSIVE`、`MEDICAL/STRESSED`。
- 周预算和月预算各一条。
- 至少一条预算超阈值提醒和一条实际超预算场景。
