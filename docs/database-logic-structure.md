# EveryCent 数据库逻辑结构

本文档按系统详细设计文档中的 MySQL 逻辑表结构整理。

## 表结构

### ledger

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(100) | NOT NULL |
| description | VARCHAR(500) | NULL |
| creator_id | BIGINT | FK -> jhi_user.id, NOT NULL |
| default_currency | VARCHAR(10) | NOT NULL, default CNY |
| current_month_balance | DECIMAL(19,2) | NOT NULL, default 0 |
| created_date | DATETIME(6) | NOT NULL |
| last_modified_date | DATETIME(6) | NULL |

索引：`idx_ledger_creator_id(creator_id)`。

### user_ledger_permission

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK -> jhi_user.id, NOT NULL |
| ledger_id | BIGINT | FK -> ledger.id, NOT NULL |
| permission_level | VARCHAR(30) | NOT NULL |
| status | VARCHAR(30) | NOT NULL, default ACTIVE |
| invited_by_id | BIGINT | FK -> jhi_user.id, NULL |
| created_date | DATETIME(6) | NOT NULL |

约束/索引：`uk_user_ledger(user_id, ledger_id)`，`idx_user_ledger_permission_user_id(user_id)`。

### behavior_tag

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| code | VARCHAR(50) | UNIQUE, NOT NULL |
| name | VARCHAR(50) | NOT NULL |
| system_default | BOOLEAN | NOT NULL |
| creator_id | BIGINT | FK -> jhi_user.id, NULL |

### emotion_tag

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| code | VARCHAR(50) | UNIQUE, NOT NULL |
| name | VARCHAR(50) | NOT NULL |
| valence | VARCHAR(20) | NOT NULL |
| system_default | BOOLEAN | NOT NULL |
| creator_id | BIGINT | FK -> jhi_user.id, NULL |

### transaction_record

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| ledger_id | BIGINT | FK -> ledger.id, NOT NULL |
| creator_id | BIGINT | FK -> jhi_user.id, NOT NULL |
| amount | DECIMAL(19,2) | NOT NULL, CHECK amount > 0 |
| type | VARCHAR(20) | NOT NULL |
| behavior_tag_id | BIGINT | FK -> behavior_tag.id, NULL |
| emotion_tag_id | BIGINT | FK -> emotion_tag.id, NULL |
| transaction_date | DATE | NOT NULL |
| source | VARCHAR(30) | NOT NULL, default MANUAL |
| description | VARCHAR(500) | NULL |
| raw_input | TEXT | NULL |
| created_date | DATETIME(6) | NOT NULL |
| last_modified_date | DATETIME(6) | NULL |

索引：`idx_transaction_ledger_date(ledger_id, transaction_date)`、`idx_transaction_creator_id(creator_id)`、`idx_transaction_behavior_tag(behavior_tag_id)`、`idx_transaction_emotion_tag(emotion_tag_id)`。

### budget

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| ledger_id | BIGINT | FK -> ledger.id, NOT NULL |
| cycle | VARCHAR(20) | NOT NULL |
| period_start | DATE | NOT NULL |
| period_end | DATE | NOT NULL |
| limit_amount | DECIMAL(19,2) | NOT NULL, CHECK limit_amount > 0 |
| alert_threshold | DECIMAL(5,2) | NOT NULL, CHECK 0 <= alert_threshold <= 1 |
| enabled | BOOLEAN | NOT NULL |

约束/索引：`uk_budget_ledger_cycle_period(ledger_id, cycle, period_start, period_end)`，`idx_budget_ledger_cycle_period(ledger_id, cycle, period_start, period_end)`。

### monthly_balance

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| ledger_id | BIGINT | FK -> ledger.id, NOT NULL |
| year | INT | NOT NULL |
| month | INT | NOT NULL, CHECK 1 <= month <= 12 |
| total_income | DECIMAL(19,2) | NOT NULL |
| total_expense | DECIMAL(19,2) | NOT NULL |
| balance | DECIMAL(19,2) | NOT NULL |
| updated_date | DATETIME(6) | NOT NULL |

约束：`uk_monthly_balance_ledger_year_month(ledger_id, year, month)`。

### notification_message

| 字段 | 类型 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK -> jhi_user.id, NOT NULL |
| ledger_id | BIGINT | FK -> ledger.id, NULL |
| budget_id | BIGINT | FK -> budget.id, NULL |
| title | VARCHAR(100) | NOT NULL |
| content | TEXT | NOT NULL |
| type | VARCHAR(30) | NOT NULL |
| level | VARCHAR(30) | NOT NULL |
| is_read | BOOLEAN | NOT NULL |
| created_date | DATETIME(6) | NOT NULL |

索引：`idx_notification_message_user_read(user_id, is_read)`。

## 示例 SQL 查询

查询某账本月度收入：

```sql
SELECT COALESCE(SUM(amount), 0) AS total_income
FROM transaction_record
WHERE ledger_id = :ledgerId
  AND type = 'INCOME'
  AND transaction_date >= :monthStart
  AND transaction_date < :nextMonthStart;
```

查询某账本月度支出：

```sql
SELECT COALESCE(SUM(amount), 0) AS total_expense
FROM transaction_record
WHERE ledger_id = :ledgerId
  AND type = 'EXPENSE'
  AND transaction_date >= :monthStart
  AND transaction_date < :nextMonthStart;
```

查询某用户可访问账本：

```sql
SELECT l.*, p.permission_level
FROM ledger l
JOIN user_ledger_permission p ON p.ledger_id = l.id
WHERE p.user_id = :userId
  AND p.status = 'ACTIVE';
```

查询预算使用比例：

```sql
SELECT b.id,
       b.limit_amount,
       COALESCE(SUM(t.amount), 0) AS used_amount,
       COALESCE(SUM(t.amount), 0) / b.limit_amount AS used_ratio
FROM budget b
LEFT JOIN transaction_record t
  ON t.ledger_id = b.ledger_id
 AND t.type = 'EXPENSE'
 AND t.transaction_date >= b.period_start
 AND t.transaction_date <= b.period_end
WHERE b.id = :budgetId
GROUP BY b.id, b.limit_amount;
```

查询超过预算阈值的账本：

```sql
SELECT b.ledger_id,
       b.id AS budget_id,
       COALESCE(SUM(t.amount), 0) AS used_amount,
       b.limit_amount,
       b.alert_threshold
FROM budget b
LEFT JOIN transaction_record t
  ON t.ledger_id = b.ledger_id
 AND t.type = 'EXPENSE'
 AND t.transaction_date >= b.period_start
 AND t.transaction_date <= b.period_end
WHERE b.enabled = true
GROUP BY b.ledger_id, b.id, b.limit_amount, b.alert_threshold
HAVING COALESCE(SUM(t.amount), 0) >= b.limit_amount * b.alert_threshold;
```
