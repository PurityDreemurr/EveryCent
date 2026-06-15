# EveryCent 数据库测试

本文档用于测试 EveryCent 智能记账系统的数据库设计是否严格符合系统详细设计文档。测试只验证数据库层，不修改数据库设计，不新增设计文档之外的实体、字段或功能。

本测试方案严格围绕设计文档中的五个核心实体展开：

```text
User
Ledger
User_Ledger_Auth
Transaction
Budget
```

其中：

```text
User(user_id, username, password_hash, email)

Ledger(ledger_id, ledger_name, creator_id, create_time)

User_Ledger_Auth(id, user_id, ledger_id, permission_level)

Transaction(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)

Budget(budget_id, ledger_id, cycle, budget_amount)
```

如果项目实际物理表名因 JHipster 或命名规范与设计文档略有不同，测试时只允许做“名称映射”，不允许改变设计含义。例如，如果设计文档中的 `User` 在实际数据库中映射为 JHipster 的用户表，则测试时应以项目最终设计文档中的映射说明为准。

------

## 1. 测试目标

数据库测试目标如下：

1. 验证数据库表结构是否与设计文档一致。
2. 验证主键、外键、非空约束、唯一约束是否正确。
3. 验证五个核心实体之间的关系是否正确。
4. 验证基础 CRUD 是否可执行。
5. 验证账本共享权限表是否能表达“只读”和“读写”两类共享场景。
6. 验证收支记录能正确关联用户和账本。
7. 验证行为标签和情绪标签能随收支记录保存。
8. 验证预算数据能按周、月保存。
9. 验证预算告警查询是否可通过数据库复杂查询实现。
10. 验证每次录入行为后，本月余额是否能够按设计要求计算或更新。
11. 验证数据看板所需的收入、支出、分类统计 SQL 是否可执行。
12. 验证触发器是否符合设计要求。
13. 形成可截图、可展示、可复现实验结果。

------

## 2. 测试范围

### 2.1 本轮测试包含

本轮测试包含以下数据库层内容：

```text
1. 表结构测试
2. 字段测试
3. 主键测试
4. 外键测试
5. 非空约束测试
6. 唯一约束测试
7. CRUD 测试
8. 账本共享权限测试
9. 收支记录测试
10. 行为标签测试
11. 情绪标签测试
12. 预算测试
13. 预算告警查询测试
14. 月度余额计算测试
15. 触发器测试
16. 数据看板统计查询测试
```

### 2.2 本轮测试不包含

以下内容不属于当前数据库单独测试范围：

```text
1. 前端页面测试
2. 后端 REST API 测试
3. LLM API 调用测试
4. Excel 文件真实导出测试
5. 前端图表渲染测试
6. 用户界面操作测试
```

这些内容应在前端和后端完成后进行系统集成测试。

------

## 3. 测试前准备

### 3.1 确认当前数据库

进入 MySQL：

```bash
mysql -u everycent -p -h 127.0.0.1 everycent
```

或使用 VS Code 数据库插件连接：

```text
Host: 127.0.0.1
Port: 3306
Database: everycent
Username: everycent
Password: 按实际开发环境填写
```

### 3.2 查看当前数据库表

```sql
SHOW TABLES;
```

预期结果：

应能看到与设计文档对应的核心表：

```text
User
Ledger
User_Ledger_Auth
Transaction
Budget
```

如果实际物理表名使用小写或下划线命名，例如：

```text
user
ledger
user_ledger_auth
transaction
budget
```

则以项目最终设计文档中的物理表名为准。

### 3.3 建议使用事务测试

为避免污染数据库，建议每组测试前执行：

```sql
START TRANSACTION;
```

测试完成后，如不需要保留测试数据，执行：

```sql
ROLLBACK;
```

如果需要保留测试数据用于截图展示，执行：

```sql
COMMIT;
```

------

## 4. 表结构一致性测试

### 4.1 User 表结构测试

执行：

```sql
DESC User;
```

或使用实际物理表名：

```sql
DESC user;
```

预期字段：

```text
user_id
username
password_hash
email
```

检查点：

```text
1. user_id 是否为主键
2. username 是否存在
3. password_hash 是否存在
4. email 是否存在
5. 用户登录所需字段是否完整
```

------

### 4.2 Ledger 表结构测试

执行：

```sql
DESC Ledger;
```

预期字段：

```text
ledger_id
ledger_name
creator_id
create_time
```

检查点：

```text
1. ledger_id 是否为主键
2. ledger_name 是否存在
3. creator_id 是否存在
4. creator_id 是否外键关联 User.user_id
5. create_time 是否存在
```

------

### 4.3 User_Ledger_Auth 表结构测试

执行：

```sql
DESC User_Ledger_Auth;
```

预期字段：

```text
id
user_id
ledger_id
permission_level
```

检查点：

```text
1. id 是否为主键
2. user_id 是否外键关联 User.user_id
3. ledger_id 是否外键关联 Ledger.ledger_id
4. permission_level 是否用于区分只读和读写权限
```

------

### 4.4 Transaction 表结构测试

执行：

```sql
DESC Transaction;
```

预期字段：

```text
record_id
ledger_id
user_id
amount
type
behavior_tag
emotion_tag
record_time
```

检查点：

```text
1. record_id 是否为主键
2. ledger_id 是否外键关联 Ledger.ledger_id
3. user_id 是否外键关联 User.user_id
4. amount 是否存在
5. type 是否表示收入或支出
6. behavior_tag 是否存在
7. emotion_tag 是否存在
8. record_time 是否存在
```

------

### 4.5 Budget 表结构测试

执行：

```sql
DESC Budget;
```

预期字段：

```text
budget_id
ledger_id
cycle
budget_amount
```

检查点：

```text
1. budget_id 是否为主键
2. ledger_id 是否外键关联 Ledger.ledger_id
3. cycle 是否表示周预算或月预算
4. budget_amount 是否存在
```

------

## 5. 外键约束测试

### 5.1 Ledger.creator_id 外键测试

测试目的：

验证账本必须由合法用户创建。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(9001, '非法创建者账本测试', 999999, NOW());
```

预期结果：

```text
插入失败，数据库提示外键约束错误。
```

说明：

如果插入成功，说明 `Ledger.creator_id` 没有正确约束到 `User.user_id`。

测试后回滚：

```sql
ROLLBACK;
```

------

### 5.2 User_Ledger_Auth.user_id 外键测试

测试目的：

验证账本权限表中的用户必须真实存在。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(9001, 999999, 1, '只读');
```

预期结果：

```text
插入失败，数据库提示 user_id 外键约束错误。
```

测试后回滚：

```sql
ROLLBACK;
```

------

### 5.3 User_Ledger_Auth.ledger_id 外键测试

测试目的：

验证账本权限表中的账本必须真实存在。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(9002, 1, 999999, '只读');
```

预期结果：

```text
插入失败，数据库提示 ledger_id 外键约束错误。
```

测试后回滚：

```sql
ROLLBACK;
```

------

### 5.4 Transaction.ledger_id 外键测试

测试目的：

验证收支记录必须属于一个真实账本。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(9001, 999999, 1, 50.00, '支出', '吃饭', '平静', NOW());
```

预期结果：

```text
插入失败，数据库提示 ledger_id 外键约束错误。
```

测试后回滚：

```sql
ROLLBACK;
```

------

### 5.5 Transaction.user_id 外键测试

测试目的：

验证收支记录必须由真实用户创建。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(9002, 1, 999999, 50.00, '支出', '吃饭', '平静', NOW());
```

预期结果：

```text
插入失败，数据库提示 user_id 外键约束错误。
```

测试后回滚：

```sql
ROLLBACK;
```

------

### 5.6 Budget.ledger_id 外键测试

测试目的：

验证预算必须属于真实账本。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(9001, 999999, '月', 1000.00);
```

预期结果：

```text
插入失败，数据库提示 ledger_id 外键约束错误。
```

测试后回滚：

```sql
ROLLBACK;
```

------

## 6. 非空约束测试

### 6.1 Ledger.ledger_name 非空测试

```sql
START TRANSACTION;

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(9101, NULL, 1, NOW());
```

预期结果：

```text
插入失败，ledger_name 不允许为空。
ROLLBACK;
```

------

### 6.2 Transaction.amount 非空测试

```sql
START TRANSACTION;

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(9102, 1, 1, NULL, '支出', '吃饭', '平静', NOW());
```

预期结果：

```text
插入失败，amount 不允许为空。
ROLLBACK;
```

------

### 6.3 Transaction.type 非空测试

```sql
START TRANSACTION;

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(9103, 1, 1, 20.00, NULL, '吃饭', '平静', NOW());
```

预期结果：

```text
插入失败，type 不允许为空。
ROLLBACK;
```

------

### 6.4 Budget.budget_amount 非空测试

```sql
START TRANSACTION;

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(9104, 1, '月', NULL);
```

预期结果：

```text
插入失败，budget_amount 不允许为空。
ROLLBACK;
```

------

## 7. 合法取值测试

### 7.1 Transaction.type 合法取值测试

设计文档中 `type` 用于表示收入或支出，因此只能出现：

```text
收入
支出
```

测试非法值：

```sql
START TRANSACTION;

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(9201, 1, 1, 100.00, '未知类型', '其他', '平静', NOW());
```

预期结果：

```text
如果数据库层实现了 CHECK 或 ENUM，插入失败。
如果数据库层未限制，则该约束必须在后端 Service 层校验，并在测试报告中说明。
ROLLBACK;
```

------

### 7.2 Budget.cycle 合法取值测试

设计文档中 `cycle` 用于表示周预算或月预算，因此只能出现：

```text
周
月
```

测试非法值：

```sql
START TRANSACTION;

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(9202, 1, '年', 1000.00);
```

预期结果：

```text
如果数据库层实现了 CHECK 或 ENUM，插入失败。
如果数据库层未限制，则该约束必须在后端 Service 层校验，并在测试报告中说明。
ROLLBACK;
```

------

### 7.3 User_Ledger_Auth.permission_level 合法取值测试

设计文档中 `permission_level` 用于表示只读或读写权限，因此至少应能区分：

```text
只读
读写
```

测试非法值：

```sql
START TRANSACTION;

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(9203, 1, 1, '非法权限');
```

预期结果：

```text
如果数据库层实现了 CHECK 或 ENUM，插入失败。
如果数据库层未限制，则该约束必须在后端 Service 层校验，并在测试报告中说明。
ROLLBACK;
```

------

## 8. 基础 CRUD 测试

### 8.1 User CRUD 测试

插入测试用户：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1001, 'test_user_a', 'test_hash_a', 'test_a@example.com');

SELECT * FROM User WHERE user_id = 1001;

UPDATE User
SET email = 'test_a_new@example.com'
WHERE user_id = 1001;

SELECT * FROM User WHERE user_id = 1001;

DELETE FROM User WHERE user_id = 1001;

SELECT * FROM User WHERE user_id = 1001;
```

预期结果：

```text
1. 用户可以插入。
2. 用户可以查询。
3. 用户邮箱可以更新。
4. 用户可以删除。
5. 删除后查询不到该用户。
ROLLBACK;
```

说明：

如果项目使用 JHipster 内置用户表，实际测试时应遵守 JHipster 用户表的字段要求，不强行插入缺失字段。

------

### 8.2 Ledger CRUD 测试

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1001, 'ledger_creator', 'test_hash', 'creator@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2001, '测试账本', 1001, NOW());

SELECT * FROM Ledger WHERE ledger_id = 2001;

UPDATE Ledger
SET ledger_name = '测试账本-已修改'
WHERE ledger_id = 2001;

SELECT * FROM Ledger WHERE ledger_id = 2001;

DELETE FROM Ledger WHERE ledger_id = 2001;

SELECT * FROM Ledger WHERE ledger_id = 2001;
```

预期结果：

```text
Ledger 可以完成新增、查询、修改、删除。
ROLLBACK;
```

------

### 8.3 User_Ledger_Auth CRUD 测试

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1001, 'auth_user_a', 'test_hash', 'auth_a@example.com'),
(1002, 'auth_user_b', 'test_hash', 'auth_b@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2001, '共享账本测试', 1001, NOW());

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(3001, 1002, 2001, '只读');

SELECT * FROM User_Ledger_Auth WHERE id = 3001;

UPDATE User_Ledger_Auth
SET permission_level = '读写'
WHERE id = 3001;

SELECT * FROM User_Ledger_Auth WHERE id = 3001;

DELETE FROM User_Ledger_Auth WHERE id = 3001;

SELECT * FROM User_Ledger_Auth WHERE id = 3001;
```

预期结果：

```text
User_Ledger_Auth 可以完成新增、查询、修改、删除，并能表达只读和读写权限变化。
ROLLBACK;
```

------

### 8.4 Transaction CRUD 测试

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1001, 'record_user', 'test_hash', 'record_user@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2001, '收支记录测试账本', 1001, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4001, 2001, 1001, 50.00, '支出', '吃饭', '平静', NOW());

SELECT * FROM Transaction WHERE record_id = 4001;

UPDATE Transaction
SET amount = 60.00,
    behavior_tag = '娱乐',
    emotion_tag = '开心'
WHERE record_id = 4001;

SELECT * FROM Transaction WHERE record_id = 4001;

DELETE FROM Transaction WHERE record_id = 4001;

SELECT * FROM Transaction WHERE record_id = 4001;
```

预期结果：

```text
Transaction 可以完成新增、查询、修改、删除。
行为标签和情绪标签可以随记录保存和修改。
ROLLBACK;
```

------

### 8.5 Budget CRUD 测试

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1001, 'budget_user', 'test_hash', 'budget_user@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2001, '预算测试账本', 1001, NOW());

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(5001, 2001, '月', 1000.00);

SELECT * FROM Budget WHERE budget_id = 5001;

UPDATE Budget
SET budget_amount = 1200.00
WHERE budget_id = 5001;

SELECT * FROM Budget WHERE budget_id = 5001;

DELETE FROM Budget WHERE budget_id = 5001;

SELECT * FROM Budget WHERE budget_id = 5001;
```

预期结果：

```text
Budget 可以完成新增、查询、修改、删除。
预算周期和预算金额可以正确保存。
ROLLBACK;
```

------

## 9. 多账本共享权限测试

### 9.1 家庭监督场景：只读权限

测试目的：

验证一个用户可以被加入另一个用户创建的账本，并获得只读权限。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1101, 'family_owner', 'test_hash', 'owner@example.com'),
(1102, 'family_supervisor', 'test_hash', 'supervisor@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2101, '家庭监督账本', 1101, NOW());

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(3101, 1102, 2101, '只读');

SELECT
    u.username,
    l.ledger_name,
    a.permission_level
FROM User_Ledger_Auth a
JOIN User u ON a.user_id = u.user_id
JOIN Ledger l ON a.ledger_id = l.ledger_id
WHERE a.ledger_id = 2101;
```

预期结果：

```text
family_supervisor 对 家庭监督账本 的 permission_level 为 只读。
```

说明：

在没有后端鉴权模块时，数据库测试只验证权限关系能正确表达。只读用户尝试修改账本时是否被拦截，应在后端 Service 或 API 层测试中继续验证。

```sql
ROLLBACK;
```

------

### 9.2 情侣共同记账场景：读写权限

测试目的：

验证多个用户可以共用同一个账本，并具备读写权限。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1201, 'couple_user_a', 'test_hash', 'a@example.com'),
(1202, 'couple_user_b', 'test_hash', 'b@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2201, '情侣共同账本', 1201, NOW());

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(3201, 1201, 2201, '读写'),
(3202, 1202, 2201, '读写');

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4201, 2201, 1201, 20.00, '支出', '吃饭', '平静', NOW()),
(4202, 2201, 1202, 35.00, '支出', '娱乐', '开心', NOW());

SELECT
    l.ledger_name,
    u.username,
    t.amount,
    t.type,
    t.behavior_tag,
    t.emotion_tag,
    t.record_time
FROM Transaction t
JOIN User u ON t.user_id = u.user_id
JOIN Ledger l ON t.ledger_id = l.ledger_id
WHERE t.ledger_id = 2201;
```

预期结果：

```text
同一个账本下可以看到两个不同用户写入的收支记录。
ROLLBACK;
```

------

## 10. 收支记录与标签测试

### 10.1 行为标签测试

测试目的：

验证不同消费场景能通过 `behavior_tag` 记录。

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1301, 'tag_user', 'test_hash', 'tag@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2301, '标签测试账本', 1301, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4301, 2301, 1301, 30.00, '支出', '吃饭', '平静', NOW()),
(4302, 2301, 1301, 80.00, '支出', '娱乐', '开心', NOW()),
(4303, 2301, 1301, 5000.00, '收入', '工资收入', '开心', NOW());

SELECT
    behavior_tag,
    COUNT(*) AS record_count,
    SUM(amount) AS total_amount
FROM Transaction
WHERE ledger_id = 2301
GROUP BY behavior_tag;
```

预期结果：

```text
能够按行为标签统计记录数量和金额。
ROLLBACK;
```

------

### 10.2 情绪标签测试

测试目的：

验证消费记录能保存情绪标签，并支持情绪维度统计。

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1401, 'emotion_user', 'test_hash', 'emotion@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2401, '情绪标签测试账本', 1401, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4401, 2401, 1401, 100.00, '支出', '购物', '冲动', NOW()),
(4402, 2401, 1401, 60.00, '支出', '吃饭', '开心', NOW()),
(4403, 2401, 1401, 200.00, '支出', '娱乐', '后悔', NOW());

SELECT
    emotion_tag,
    COUNT(*) AS record_count,
    SUM(amount) AS total_amount
FROM Transaction
WHERE ledger_id = 2401
GROUP BY emotion_tag;
```

预期结果：

```text
能够按情绪标签统计记录数量和金额。
ROLLBACK;
```

------

## 11. 预算测试

### 11.1 周预算测试

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1501, 'weekly_budget_user', 'test_hash', 'weekly@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2501, '周预算测试账本', 1501, NOW());

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(5501, 2501, '周', 300.00);

SELECT * FROM Budget WHERE ledger_id = 2501 AND cycle = '周';
```

预期结果：

```text
周预算可以正确保存和查询。
ROLLBACK;
```

------

### 11.2 月预算测试

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1601, 'monthly_budget_user', 'test_hash', 'monthly@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2601, '月预算测试账本', 1601, NOW());

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(5601, 2601, '月', 1500.00);

SELECT * FROM Budget WHERE ledger_id = 2601 AND cycle = '月';
```

预期结果：

```text
月预算可以正确保存和查询。
ROLLBACK;
```

------

## 12. 预算告警查询测试

测试目的：

验证数据库可以通过复杂查询判断某账本消费是否超过预算。

测试数据：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1701, 'warning_user', 'test_hash', 'warning@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2701, '预算告警测试账本', 1701, NOW());

INSERT INTO Budget
(budget_id, ledger_id, cycle, budget_amount)
VALUES
(5701, 2701, '月', 500.00);

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4701, 2701, 1701, 200.00, '支出', '吃饭', '平静', '2026-06-01 12:00:00'),
(4702, 2701, 1701, 250.00, '支出', '娱乐', '开心', '2026-06-05 20:00:00'),
(4703, 2701, 1701, 100.00, '支出', '购物', '冲动', '2026-06-10 18:00:00');
```

预算使用情况查询：

```sql
SELECT
    l.ledger_id,
    l.ledger_name,
    b.cycle,
    b.budget_amount,
    SUM(t.amount) AS total_expense,
    SUM(t.amount) / b.budget_amount AS usage_ratio
FROM Ledger l
JOIN Budget b ON l.ledger_id = b.ledger_id
JOIN Transaction t ON l.ledger_id = t.ledger_id
WHERE l.ledger_id = 2701
  AND b.cycle = '月'
  AND t.type = '支出'
GROUP BY
    l.ledger_id,
    l.ledger_name,
    b.cycle,
    b.budget_amount;
```

预期结果：

```text
total_expense = 550.00
budget_amount = 500.00
usage_ratio > 1
说明该账本已经超过月预算。
```

预算告警筛选查询：

```sql
SELECT
    l.ledger_id,
    l.ledger_name,
    b.budget_amount,
    SUM(t.amount) AS total_expense
FROM Ledger l
JOIN Budget b ON l.ledger_id = b.ledger_id
JOIN Transaction t ON l.ledger_id = t.ledger_id
WHERE b.cycle = '月'
  AND t.type = '支出'
GROUP BY
    l.ledger_id,
    l.ledger_name,
    b.budget_amount
HAVING SUM(t.amount) > b.budget_amount;
```

预期结果：

```text
能够查询出预算告警测试账本。
ROLLBACK;
```

说明：

该查询结果可以作为后续 AI 个性化提醒模块的输入数据，但当前测试不调用 LLM。

------

## 13. 本月余额计算测试

测试目的：

验证数据库能支持“每次录入行为后，自动计算本月余额”的设计要求。

测试数据：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1801, 'balance_user', 'test_hash', 'balance@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2801, '余额计算测试账本', 1801, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4801, 2801, 1801, 3000.00, '收入', '工资收入', '开心', '2026-06-01 09:00:00'),
(4802, 2801, 1801, 50.00, '支出', '吃饭', '平静', '2026-06-02 12:00:00'),
(4803, 2801, 1801, 100.00, '支出', '娱乐', '开心', '2026-06-03 20:00:00');
```

余额查询：

```sql
SELECT
    ledger_id,
    SUM(CASE WHEN type = '收入' THEN amount ELSE 0 END) AS total_income,
    SUM(CASE WHEN type = '支出' THEN amount ELSE 0 END) AS total_expense,
    SUM(CASE WHEN type = '收入' THEN amount ELSE -amount END) AS monthly_balance
FROM Transaction
WHERE ledger_id = 2801
  AND record_time >= '2026-06-01'
  AND record_time < '2026-07-01'
GROUP BY ledger_id;
```

预期结果：

```text
total_income = 3000.00
total_expense = 150.00
monthly_balance = 2850.00
```

如果设计文档中已经实现触发器自动维护余额字段，应继续执行触发器验证：

```text
1. 插入 Transaction 前查询余额字段。
2. 插入收入或支出记录。
3. 再次查询余额字段。
4. 验证余额字段是否自动变化。
```

注意：

触发器测试必须以设计文档中指定的余额存储位置为准，不允许临时新增余额表或汇总表。

```sql
ROLLBACK;
```

------

## 14. 触发器测试

测试目的：

验证设计文档中要求的触发器是否生效。

设计要求：

```text
当插入新的 Transaction 时，触发器自动更新账本的当月总余额。
```

测试步骤：

```text
1. 查询指定账本插入记录前的当月余额。
2. 插入一条收入 Transaction。
3. 查询插入后的当月余额。
4. 插入一条支出 Transaction。
5. 再次查询当月余额。
6. 验证余额变化是否符合收入增加、支出减少的规则。
```

测试模板：

```sql
START TRANSACTION;

-- 1. 准备用户和账本
INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(1901, 'trigger_user', 'test_hash', 'trigger@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(2901, '触发器测试账本', 1901, NOW());

-- 2. 查询插入前余额
-- 此处按照设计文档中指定的余额字段或余额查询方式填写
-- 示例：
-- SELECT ... FROM ... WHERE ledger_id = 2901;

-- 3. 插入收入
INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4901, 2901, 1901, 1000.00, '收入', '工资收入', '开心', '2026-06-01 09:00:00');

-- 4. 查询收入插入后的余额
-- 按照设计文档中触发器更新的位置查询

-- 5. 插入支出
INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(4902, 2901, 1901, 100.00, '支出', '吃饭', '平静', '2026-06-01 12:00:00');

-- 6. 查询支出插入后的余额
-- 按照设计文档中触发器更新的位置查询
```

预期结果：

```text
插入收入后，本月余额增加 1000.00。
插入支出后，本月余额减少 100.00。
最终余额变化符合 Transaction.type 与 amount 的计算规则。
ROLLBACK;
```

说明：

如果当前阶段尚未实现触发器，则测试报告中应记录：

```text
触发器尚未实现，当前通过 SQL 聚合查询验证余额计算逻辑。
后续需要补充触发器或在后端 Service 层实现自动更新。
```

该说明不是修改设计，而是记录当前实现状态。

------

## 15. 多用户同步测试

测试目的：

验证多个用户共同操作同一个账本时，数据库中数据能统一保存到同一个 Ledger 下。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(2001, 'sync_user_a', 'test_hash', 'sync_a@example.com'),
(2002, 'sync_user_b', 'test_hash', 'sync_b@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(3001, '多用户同步测试账本', 2001, NOW());

INSERT INTO User_Ledger_Auth
(id, user_id, ledger_id, permission_level)
VALUES
(4001, 2001, 3001, '读写'),
(4002, 2002, 3001, '读写');

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(5001, 3001, 2001, 30.00, '支出', '吃饭', '平静', NOW()),
(5002, 3001, 2002, 120.00, '支出', '购物', '开心', NOW());

SELECT
    l.ledger_name,
    u.username,
    t.amount,
    t.type,
    t.behavior_tag,
    t.emotion_tag
FROM Transaction t
JOIN User u ON t.user_id = u.user_id
JOIN Ledger l ON t.ledger_id = l.ledger_id
WHERE l.ledger_id = 3001
ORDER BY t.record_time;
```

预期结果：

```text
两个用户的记录都归属于同一个账本。
可以通过 ledger_id 查询到完整账本记录。
ROLLBACK;
```

并发补充测试：

如果需要演示数据库事务隔离，可以打开两个 MySQL 终端窗口：

```text
终端 A：开启事务，插入记录但不提交。
终端 B：查询同一账本记录。
终端 A：提交事务。
终端 B：再次查询。
```

用于说明多用户同步时数据库事务的一致性行为。

------

## 16. 数据看板查询测试

数据看板需要支持：

```text
1. 收入统计
2. 支出统计
3. 消费-剩余饼图
4. 收入-支出数据分析
5. 行为标签分类统计
6. 情绪标签分类统计
```

### 16.1 收入支出总览

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(2101, 'dashboard_user', 'test_hash', 'dashboard@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(3101, '数据看板测试账本', 2101, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(5101, 3101, 2101, 5000.00, '收入', '工资收入', '开心', '2026-06-01 09:00:00'),
(5102, 3101, 2101, 30.00, '支出', '吃饭', '平静', '2026-06-02 12:00:00'),
(5103, 3101, 2101, 200.00, '支出', '娱乐', '开心', '2026-06-03 20:00:00'),
(5104, 3101, 2101, 100.00, '支出', '交通', '平静', '2026-06-04 08:00:00');

SELECT
    type,
    SUM(amount) AS total_amount
FROM Transaction
WHERE ledger_id = 3101
  AND record_time >= '2026-06-01'
  AND record_time < '2026-07-01'
GROUP BY type;
```

预期结果：

```text
收入合计为 5000.00。
支出合计为 330.00。
```

------

### 16.2 行为标签分类统计

```sql
SELECT
    behavior_tag,
    SUM(amount) AS total_amount
FROM Transaction
WHERE ledger_id = 3101
  AND type = '支出'
  AND record_time >= '2026-06-01'
  AND record_time < '2026-07-01'
GROUP BY behavior_tag
ORDER BY total_amount DESC;
```

预期结果：

```text
可以按行为标签统计各类支出金额。
```

------

### 16.3 情绪标签分类统计

```sql
SELECT
    emotion_tag,
    COUNT(*) AS record_count,
    SUM(amount) AS total_amount
FROM Transaction
WHERE ledger_id = 3101
  AND type = '支出'
GROUP BY emotion_tag
ORDER BY total_amount DESC;
```

预期结果：

```text
可以按情绪标签统计消费次数和消费金额。
```

------

### 16.4 消费-剩余饼图数据

```sql
SELECT
    SUM(CASE WHEN type = '支出' THEN amount ELSE 0 END) AS total_expense,
    SUM(CASE WHEN type = '收入' THEN amount ELSE -amount END) AS remaining_balance
FROM Transaction
WHERE ledger_id = 3101
  AND record_time >= '2026-06-01'
  AND record_time < '2026-07-01';
```

预期结果：

```text
total_expense = 330.00
remaining_balance = 4670.00
ROLLBACK;
```

------

## 17. 数据导出查询测试

虽然当前没有后端 Excel 导出功能，但数据库层可以先验证导出所需数据是否能被查询出来。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(2201, 'export_user', 'test_hash', 'export@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(3201, '导出测试账本', 2201, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(5201, 3201, 2201, 3000.00, '收入', '工资收入', '开心', '2026-06-01 09:00:00'),
(5202, 3201, 2201, 35.00, '支出', '吃饭', '平静', '2026-06-02 12:00:00');

SELECT
    l.ledger_name AS 账本名称,
    u.username AS 记录用户,
    t.amount AS 金额,
    t.type AS 收支类型,
    t.behavior_tag AS 行为标签,
    t.emotion_tag AS 情绪标签,
    t.record_time AS 记录时间
FROM Transaction t
JOIN Ledger l ON t.ledger_id = l.ledger_id
JOIN User u ON t.user_id = u.user_id
WHERE t.ledger_id = 3201
ORDER BY t.record_time;
```

预期结果：

```text
查询结果包含 Excel 导出所需字段。
ROLLBACK;
```

------

## 18. AI 解析结果入库合法性测试

当前不测试 LLM 调用，只测试“AI 解析后形成的结构化结果”能否作为 Transaction 数据入库。

测试目的：

验证自然语言记账解析后的字段：

```text
amount
type
behavior_tag
emotion_tag
record_time
```

可以正确保存到 `Transaction` 表。

测试 SQL：

```sql
START TRANSACTION;

INSERT INTO User
(user_id, username, password_hash, email)
VALUES
(2301, 'ai_record_user', 'test_hash', 'ai_record@example.com');

INSERT INTO Ledger
(ledger_id, ledger_name, creator_id, create_time)
VALUES
(3301, 'AI解析入库测试账本', 2301, NOW());

INSERT INTO Transaction
(record_id, ledger_id, user_id, amount, type, behavior_tag, emotion_tag, record_time)
VALUES
(5301, 3301, 2301, 50.00, '支出', '吃饭', '开心', NOW());

SELECT
    amount,
    type,
    behavior_tag,
    emotion_tag,
    record_time
FROM Transaction
WHERE record_id = 5301;
```

预期结果：

```text
amount = 50.00
type = 支出
behavior_tag = 吃饭
emotion_tag = 开心
ROLLBACK;
```

说明：

此测试只验证数据库能接收结构化 Transaction 数据，不测试 LLM API。

------

## 19. 测试截图要求

课程设计报告中建议保留以下截图：

```text
1. SHOW TABLES 结果截图
2. DESC User 结果截图
3. DESC Ledger 结果截图
4. DESC User_Ledger_Auth 结果截图
5. DESC Transaction 结果截图
6. DESC Budget 结果截图
7. 外键约束查询结果截图
8. 插入非法外键时报错截图
9. 插入只读权限记录截图
10. 插入读写权限记录截图
11. 多用户共同账本查询结果截图
12. 预算告警查询结果截图
13. 月度余额统计查询结果截图
14. 行为标签统计查询结果截图
15. 情绪标签统计查询结果截图
16. 触发器测试前后余额变化截图
```

------

## 20. 测试结果记录模板

| 测试编号 | 测试内容        | 测试 SQL / 操作                    | 预期结果         | 实际结果 | 是否通过 | 备注 |
| -------- | --------------- | ---------------------------------- | ---------------- | -------- | -------- | ---- |
| DB-01    | 表结构测试      | SHOW TABLES                        | 核心表存在       |          |          |      |
| DB-02    | User 表字段测试 | DESC User                          | 字段符合设计     |          |          |      |
| DB-03    | Ledger 外键测试 | 插入非法 creator_id                | 插入失败         |          |          |      |
| DB-04    | 权限表测试      | 插入只读/读写权限                  | 权限可区分       |          |          |      |
| DB-05    | 收支记录测试    | 插入 Transaction                   | 记录保存成功     |          |          |      |
| DB-06    | 行为标签测试    | GROUP BY behavior_tag              | 分类统计正确     |          |          |      |
| DB-07    | 情绪标签测试    | GROUP BY emotion_tag               | 分类统计正确     |          |          |      |
| DB-08    | 预算测试        | 插入 Budget                        | 预算保存成功     |          |          |      |
| DB-09    | 预算告警测试    | HAVING SUM(amount) > budget_amount | 超预算账本被查出 |          |          |      |
| DB-10    | 余额计算测试    | CASE WHEN 聚合                     | 收入-支出正确    |          |          |      |
| DB-11    | 触发器测试      | 插入 Transaction 后查余额          | 余额自动更新     |          |          |      |
| DB-12    | 多用户同步测试  | 两用户写入同一账本                 | 记录统一归属账本 |          |          |      |

------

## 21. 通过标准

数据库测试通过标准如下：

```text
1. 五个核心实体对应的表均存在。
2. 所有设计字段均存在。
3. 主键约束正确。
4. 外键约束正确。
5. 必要字段非空约束正确。
6. 用户、账本、权限、收支记录、预算之间关系正确。
7. 可以插入合法测试数据。
8. 非法外键数据会被拒绝。
9. 只读和读写权限可以通过 User_Ledger_Auth 表区分。
10. 同一账本可以被多个用户共同关联。
11. Transaction 可以记录金额、收支类型、行为标签、情绪标签和时间。
12. Budget 可以记录周预算和月预算。
13. 数据库查询可以识别预算超支。
14. 数据库查询可以计算本月余额。
15. 数据库查询可以支撑数据看板。
16. 如果设计中实现触发器，则触发器能在插入 Transaction 后更新余额。
```

------

## 22. 不通过时的记录方式

如果某项测试失败，不要直接修改设计，应先记录：

```text
1. 失败的测试编号
2. 失败 SQL
3. 数据库报错信息
4. 预期结果
5. 实际结果
6. 判断原因
7. 是否属于实现问题，还是设计文档未定义
```

示例：

```text
测试编号：DB-09
测试内容：预算告警查询
预期结果：当支出总额大于预算金额时，应查询出该账本
实际结果：查询结果为空
原因分析：Transaction.type 使用值与设计文档不一致，测试数据中使用“支出”，实际数据库中使用“EXPENSE”
处理方式：不修改设计，统一测试数据取值为设计文档规定值或项目最终枚举值
```

------

## 23. 最终结论模板

测试完成后，可以在报告中写：

```text
通过数据库单元测试，EveryCent 系统的核心数据库结构能够覆盖用户、账本、账本共享权限、收支记录与预算管理五类实体。数据库能够通过外键约束保证用户、账本和记录之间的引用完整性；通过 User_Ledger_Auth 表表达家庭监督场景下的只读权限与情侣共同记账场景下的读写权限；通过 Transaction 表记录金额、收支类型、行为标签、情绪标签和记录时间；通过 Budget 表支持周预算和月预算管理。进一步通过复杂查询验证了预算超支识别、月度余额计算、行为标签统计、情绪标签统计和数据看板所需数据查询的可行性。测试结果表明，数据库设计能够支撑系统详细设计文档中的核心业务需求。
```