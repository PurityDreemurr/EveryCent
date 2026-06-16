# EveryCent 后端详细设计文档（不包含LLM控制器）

> 建议保存路径：`docs/backend-design.md`
> 适用范围：EveryCent 智能记账系统常规后端模块
> 不包含范围：LLM 调用、自然语言解析、Prompt 构造、AI 结果解析
> 技术基座：JHipster Monolithic Application + Spring Boot + Spring Data JPA + MySQL + JWT
> Java 包名：`com.everycent`

------

## 1. 文档目标

本文档用于指导 EveryCent 系统中 **常规后端模块** 的开发，明确后端开发人员需要实现的类、DTO、Repository、Service、REST API、权限校验、数据库访问逻辑、业务流程和测试重点。

本文档只覆盖普通业务闭环：

```text
用户登录
账本管理
账本成员权限
手动收支记录
行为标签
情绪标签
预算管理
预算状态计算
月度余额计算
数据看板
Excel 导出
消息提醒
```

本文档不覆盖 AI / LLM 相关实现。

------

## 2. 后端职责边界

### 2.1 常规后端负责内容

常规后端负责：

```text
1. 接收前端 HTTP 请求。
2. 校验 JWT 登录状态。
3. 获取当前登录用户。
4. 校验用户是否有账本访问权限。
5. 处理账本、权限、收支记录、预算、标签、通知等业务逻辑。
6. 调用 Repository 读写 MySQL 数据库。
7. 计算预算使用情况。
8. 计算月度收入、支出、余额。
9. 为数据看板提供聚合统计结果。
10. 导出 Excel。
11. 返回统一 JSON 响应。
12. 抛出规范化业务异常。
```

### 2.2 常规后端不负责内容

以下内容不属于本文档范围：

```text
1. LLM API 调用。
2. Prompt 构造。
3. 自然语言记账解析。
4. LLM 返回 JSON 解析。
5. AI 个性化文案生成。
6. TransactionParseResultDTO。
7. AiAlertResult。
8. LlmClient。
9. LlmParsingService。
10. LlmResource。
```

### 2.3 常规后端与 AI 模块的边界

普通后端与 AI 模块之间的边界如下：

```text
AI 模块负责：
自然语言文本 -> 结构化解析结果

常规后端负责：
结构化解析结果 -> 权限校验 -> 字段合法性校验 -> 写入数据库
```

也就是说：

```text
AI 模块只负责“解析和建议”
常规后端负责“校验和入库”
```

后续 AI 模块可以把解析出的金额、类型、标签、日期、描述转换为 `TransactionRecordDTO`，再调用普通记账 Service 完成入库。

------

## 3. 后端总体分层结构

EveryCent 后端采用典型 Spring Boot 分层结构：

```text
Controller / Resource 层
    ↓
Service 层
    ↓
Repository 层
    ↓
Domain / Entity
    ↓
MySQL
```

对应包结构：

```text
src/main/java/com/everycent/
├── domain/
├── domain/enumeration/
├── repository/
├── service/
├── service/dto/
├── service/mapper/
├── web/rest/
└── security/
```

建议后端开发人员主要修改：

```text
src/main/java/com/everycent/domain/
src/main/java/com/everycent/domain/enumeration/
src/main/java/com/everycent/repository/
src/main/java/com/everycent/service/
src/main/java/com/everycent/service/dto/
src/main/java/com/everycent/service/mapper/
src/main/java/com/everycent/web/rest/
```

不要修改：

```text
src/main/java/com/everycent/llm/
src/main/webapp/
src/main/resources/config/liquibase/
```

除非与对应负责人确认。

------

## 4. 常规后端模块划分

| 模块         | Resource                               | Service                      | Repository                                      | 主要职责                                |
| ------------ | -------------------------------------- | ---------------------------- | ----------------------------------------------- | --------------------------------------- |
| 账本模块     | `LedgerResource`                       | `LedgerService`              | `LedgerRepository`                              | 账本 CRUD、账本详情、当前用户账本列表   |
| 权限模块     | 无独立 Resource，可并入 LedgerResource | `LedgerPermissionService`    | `UserLedgerPermissionRepository`                | 判断用户可读、可写、是否 OWNER          |
| 收支记录模块 | `TransactionRecordResource`            | `TransactionRecordService`   | `TransactionRecordRepository`                   | 手动收支记录 CRUD、筛选、分页、余额更新 |
| 标签模块     | `TagResource`                          | `TagService` 可选            | `BehaviorTagRepository`、`EmotionTagRepository` | 查询行为标签和情绪标签                  |
| 预算模块     | `BudgetResource`                       | `BudgetService`              | `BudgetRepository`                              | 预算 CRUD、预算状态计算、超支检测       |
| 数据看板模块 | `DashboardResource`                    | `DashboardService`           | `TransactionRecordRepository` 等                | 聚合统计、趋势、分类占比                |
| 导出模块     | `ExportResource`                       | `ExcelExportService`         | `TransactionRecordRepository`                   | 导出 Excel 账单                         |
| 通知模块     | `NotificationResource`                 | `NotificationService`        | `NotificationMessageRepository`                 | 预算提醒、共享提醒、已读状态            |
| 月度余额模块 | 无独立 Resource                        | `MonthlyBalanceService` 可选 | `MonthlyBalanceRepository`                      | 月度余额快照维护                        |

------

## 5. Domain 领域类设计

### 5.1 Ledger

位置：

```text
src/main/java/com/everycent/domain/Ledger.java
```

职责：

```text
账本实体。
用于保存账本名称、描述、创建者、默认货币、当前月余额、创建时间和更新时间。
```

主要字段：

```java
private Long id;
private String name;
private String description;
private String defaultCurrency;
private BigDecimal currentMonthBalance;
private Instant createdDate;
private Instant lastModifiedDate;
private User creator;
```

建议注解：

```java
@NotNull
@Size(max = 100)
@DecimalMin("0")
@ManyToOne(optional = false)
```

数据库映射：

```text
表名：ledger
主键：id
外键：creator_id -> jhi_user.id
```

关联关系：

```text
一个 User 可以创建多个 Ledger。
一个 Ledger 可以有多个 UserLedgerPermission。
一个 Ledger 可以有多条 TransactionRecord。
一个 Ledger 可以有多个 Budget。
一个 Ledger 可以有多个月度余额记录。
```

------

### 5.2 UserLedgerPermission

位置：

```text
src/main/java/com/everycent/domain/UserLedgerPermission.java
```

职责：

```text
账本权限实体。
用于描述某个用户对某个账本拥有何种权限。
```

主要字段：

```java
private Long id;
private PermissionLevel permissionLevel;
private PermissionStatus status;
private Instant createdDate;
private User user;
private User invitedBy;
private Ledger ledger;
```

数据库映射：

```text
表名：user_ledger_permission
主键：id
外键：user_id -> jhi_user.id
外键：ledger_id -> ledger.id
外键：invited_by_id -> jhi_user.id
唯一约束：(user_id, ledger_id)
```

权限说明：

| 权限         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| `OWNER`      | 账本所有者，拥有全部权限                                     |
| `READ_WRITE` | 可查看、可新增、可修改、可删除记录，但不能管理成员和删除账本 |
| `READ_ONLY`  | 只读，只能查看账本、记录、预算、看板和导出                   |

状态说明：

| 状态      | 说明       |
| --------- | ---------- |
| `ACTIVE`  | 权限有效   |
| `PENDING` | 邀请待确认 |
| `REVOKED` | 权限已撤销 |

------

### 5.3 TransactionRecord

位置：

```text
src/main/java/com/everycent/domain/TransactionRecord.java
```

职责：

```text
收支记录实体。
用于保存某个用户在某个账本中的收入或支出。
```

主要字段：

```java
private Long id;
private BigDecimal amount;
private TransactionType type;
private String description;
private LocalDate transactionDate;
private RecordSource source;
private String rawInput;
private Instant createdDate;
private Instant lastModifiedDate;
private Ledger ledger;
private User creator;
private BehaviorTag behaviorTag;
private EmotionTag emotionTag;
```

数据库映射：

```text
表名：transaction_record
主键：id
外键：ledger_id -> ledger.id
外键：creator_id -> jhi_user.id
外键：behavior_tag_id -> behavior_tag.id
外键：emotion_tag_id -> emotion_tag.id
```

字段说明：

| 字段              | 说明                                 |
| ----------------- | ------------------------------------ |
| `amount`          | 金额，必须大于 0                     |
| `type`            | 收入或支出，`INCOME` / `EXPENSE`     |
| `transactionDate` | 交易日期                             |
| `source`          | 来源，普通手动记账为 `MANUAL`        |
| `rawInput`        | 自然语言原始输入，普通手动记账可为空 |
| `behaviorTag`     | 行为标签                             |
| `emotionTag`      | 情绪标签                             |

说明：

```text
普通后端当前只需要处理 source = MANUAL。
source = NATURAL_LANGUAGE 由后续 AI 模块解析后再调用普通记账逻辑。
```

------

### 5.4 Budget

位置：

```text
src/main/java/com/everycent/domain/Budget.java
```

职责：

```text
预算实体。
用于保存某个账本在某个时间周期内的预算金额和告警阈值。
```

主要字段：

```java
private Long id;
private BudgetCycle cycle;
private LocalDate periodStart;
private LocalDate periodEnd;
private BigDecimal limitAmount;
private BigDecimal alertThreshold;
private Boolean enabled;
private Ledger ledger;
```

数据库映射：

```text
表名：budget
主键：id
外键：ledger_id -> ledger.id
唯一约束：(ledger_id, cycle, period_start, period_end)
```

字段说明：

| 字段             | 说明                |
| ---------------- | ------------------- |
| `cycle`          | 周预算或月预算      |
| `periodStart`    | 周期开始日期        |
| `periodEnd`      | 周期结束日期        |
| `limitAmount`    | 预算上限            |
| `alertThreshold` | 预警阈值，例如 0.80 |
| `enabled`        | 是否启用            |

说明：

```text
usedAmount、remainingAmount、usedRatio 不作为 Budget 实体字段。
这些值由 BudgetService 根据 transaction_record 聚合计算后填入 BudgetDTO。
```

------

### 5.5 BehaviorTag

位置：

```text
src/main/java/com/everycent/domain/BehaviorTag.java
```

职责：

```text
行为标签实体。
用于描述消费或收入场景。
```

主要字段：

```java
private Long id;
private String code;
private String name;
private Boolean systemDefault;
private User creator;
```

数据库映射：

```text
表名：behavior_tag
主键：id
唯一约束：code
外键：creator_id -> jhi_user.id，可为空
```

默认行为标签建议：

```text
FOOD 餐饮
TRANSPORT 交通
SHOPPING 购物
ENTERTAINMENT 娱乐
STUDY 学习
MEDICAL 医疗
SALARY 工资
PART_TIME 兼职
OTHER 其他
```

------

### 5.6 EmotionTag

位置：

```text
src/main/java/com/everycent/domain/EmotionTag.java
```

职责：

```text
情绪标签实体。
用于描述记账时的情绪状态。
```

主要字段：

```java
private Long id;
private String code;
private String name;
private String valence;
private Boolean systemDefault;
private User creator;
```

数据库映射：

```text
表名：emotion_tag
主键：id
唯一约束：code
外键：creator_id -> jhi_user.id，可为空
```

默认情绪标签建议：

```text
HAPPY 开心
CALM 平静
IMPULSIVE 冲动
ANXIOUS 焦虑
REGRET 后悔
STRESSED 压力
NONE 无明显情绪
```

`valence` 取值建议：

```text
POSITIVE
NEUTRAL
NEGATIVE
```

------

### 5.7 MonthlyBalance

位置：

```text
src/main/java/com/everycent/domain/MonthlyBalance.java
```

职责：

```text
月度余额快照实体。
用于保存某账本某年某月的收入合计、支出合计和余额。
```

主要字段：

```java
private Long id;
private Integer year;
private Integer month;
private BigDecimal totalIncome;
private BigDecimal totalExpense;
private BigDecimal balance;
private Instant updatedDate;
private Ledger ledger;
```

数据库映射：

```text
表名：monthly_balance
主键：id
外键：ledger_id -> ledger.id
唯一约束：(ledger_id, year, month)
```

说明：

```text
MonthlyBalance 可以由数据库触发器维护，也可以由 TransactionRecordService 在新增、修改、删除记录后调用 MonthlyBalanceService 重算。
如果数据库负责人已经实现触发器，后端只需要查询结果。
如果未实现触发器，后端需要在 Service 层维护。
```

------

### 5.8 NotificationMessage

位置：

```text
src/main/java/com/everycent/domain/NotificationMessage.java
```

职责：

```text
消息提醒实体。
用于保存预算提醒、记账提醒、账本共享提醒等消息。
```

主要字段：

```java
private Long id;
private String title;
private String content;
private NotificationType type;
private AlertLevel level;
private Boolean read;
private Instant createdDate;
private User user;
private Ledger ledger;
private Budget budget;
```

数据库映射：

```text
表名：notification_message
主键：id
外键：user_id -> jhi_user.id
外键：ledger_id -> ledger.id
外键：budget_id -> budget.id
```

------

## 6. 枚举设计

建议位置：

```text
src/main/java/com/everycent/domain/enumeration/
```

### 6.1 PermissionLevel

```java
public enum PermissionLevel {
    OWNER,
    READ_WRITE,
    READ_ONLY
}
```

### 6.2 PermissionStatus

```java
public enum PermissionStatus {
    ACTIVE,
    PENDING,
    REVOKED
}
```

### 6.3 TransactionType

```java
public enum TransactionType {
    INCOME,
    EXPENSE
}
```

### 6.4 RecordSource

```java
public enum RecordSource {
    MANUAL,
    NATURAL_LANGUAGE
}
```

说明：

```text
普通后端当前使用 MANUAL。
NATURAL_LANGUAGE 仅作为后续 AI 模块写入记录时的来源标记。
```

### 6.5 BudgetCycle

```java
public enum BudgetCycle {
    WEEKLY,
    MONTHLY
}
```

### 6.6 NotificationType

```java
public enum NotificationType {
    BUDGET_ALERT,
    REMINDER,
    SHARE_INVITE
}
```

### 6.7 AlertLevel

```java
public enum AlertLevel {
    INFO,
    WARNING,
    DANGER
}
```

------

## 7. DTO 设计

DTO 建议位置：

```text
src/main/java/com/everycent/service/dto/
```

DTO 用于：

```text
1. 接收前端请求。
2. 返回前端响应。
3. 屏蔽 Entity 内部结构。
4. 补充数据库中没有、但前端需要展示的计算字段。
```

不要把 AI 解析 DTO 放在这里。
`TransactionParseResultDTO`、`AiAlertResultDTO` 等 AI DTO 应放入 LLM 模块。

------

### 7.1 LedgerDTO

位置：

```text
src/main/java/com/everycent/service/dto/LedgerDTO.java
```

用途：

```text
账本列表
账本详情
创建账本
修改账本
展示当前用户权限
展示成员数量
```

字段：

```java
private Long id;
private String name;
private String description;
private String defaultCurrency;
private BigDecimal currentMonthBalance;
private PermissionLevel currentUserPermission;
private Integer memberCount;
```

字段来源：

| 字段                    | 来源                                                         |
| ----------------------- | ------------------------------------------------------------ |
| `id`                    | `ledger.id`                                                  |
| `name`                  | `ledger.name`                                                |
| `description`           | `ledger.description`                                         |
| `defaultCurrency`       | `ledger.default_currency`                                    |
| `currentMonthBalance`   | `ledger.current_month_balance` 或 `monthly_balance` 计算结果 |
| `currentUserPermission` | `user_ledger_permission.permission_level`                    |
| `memberCount`           | `user_ledger_permission` 聚合统计                            |

------

### 7.2 LedgerMemberDTO

位置：

```text
src/main/java/com/everycent/service/dto/LedgerMemberDTO.java
```

用途：

```text
账本成员列表
成员权限展示
成员权限修改响应
```

字段：

```java
private Long userId;
private String login;
private String email;
private PermissionLevel permissionLevel;
private PermissionStatus status;
private Instant createdDate;
```

字段来源：

| 字段              | 来源                                      |
| ----------------- | ----------------------------------------- |
| `userId`          | `jhi_user.id`                             |
| `login`           | `jhi_user.login`                          |
| `email`           | `jhi_user.email`                          |
| `permissionLevel` | `user_ledger_permission.permission_level` |
| `status`          | `user_ledger_permission.status`           |
| `createdDate`     | `user_ledger_permission.created_date`     |

------

### 7.3 InviteMemberRequestDTO

位置：

```text
src/main/java/com/everycent/service/dto/InviteMemberRequestDTO.java
```

用途：

```text
邀请账本成员请求体
```

字段：

```java
private String loginOrEmail;
private PermissionLevel permissionLevel;
```

校验规则：

```text
loginOrEmail 不允许为空
permissionLevel 只能是 READ_WRITE 或 READ_ONLY
不能邀请为 OWNER
不能重复邀请同一个用户进入同一个账本
```

------

### 7.4 UpdateMemberPermissionDTO

位置：

```text
src/main/java/com/everycent/service/dto/UpdateMemberPermissionDTO.java
```

用途：

```text
修改账本成员权限请求体
```

字段：

```java
private PermissionLevel permissionLevel;
```

校验规则：

```text
只有 OWNER 可以修改成员权限
不能把其他成员直接改为 OWNER，除非后续设计支持转让所有权
不能修改账本创建者自己的 OWNER 权限
```

------

### 7.5 TransactionRecordDTO

位置：

```text
src/main/java/com/everycent/service/dto/TransactionRecordDTO.java
```

用途：

```text
手动新增收支记录
收支记录列表
收支记录详情
修改收支记录
Excel 导出数据来源
```

字段：

```java
private Long id;
private Long ledgerId;
private BigDecimal amount;
private TransactionType type;
private Long behaviorTagId;
private String behaviorTagName;
private Long emotionTagId;
private String emotionTagName;
private LocalDate transactionDate;
private String description;
private RecordSource source;
private String rawInput;
private String creatorLogin;
```

字段来源：

| 字段              | 来源                                  |
| ----------------- | ------------------------------------- |
| `id`              | `transaction_record.id`               |
| `ledgerId`        | `transaction_record.ledger_id`        |
| `amount`          | `transaction_record.amount`           |
| `type`            | `transaction_record.type`             |
| `behaviorTagId`   | `transaction_record.behavior_tag_id`  |
| `behaviorTagName` | `behavior_tag.name`                   |
| `emotionTagId`    | `transaction_record.emotion_tag_id`   |
| `emotionTagName`  | `emotion_tag.name`                    |
| `transactionDate` | `transaction_record.transaction_date` |
| `description`     | `transaction_record.description`      |
| `source`          | `transaction_record.source`           |
| `rawInput`        | `transaction_record.raw_input`        |
| `creatorLogin`    | `jhi_user.login`                      |

校验规则：

```text
amount 必须大于 0
type 必须为 INCOME 或 EXPENSE
ledgerId 必须存在
当前用户必须对 ledgerId 有写权限
behaviorTagId 如果传入，必须存在
emotionTagId 如果传入，必须存在
transactionDate 不允许为空
description 长度不能超过数据库限制
```

------

### 7.6 TransactionQueryDTO

位置：

```text
src/main/java/com/everycent/service/dto/TransactionQueryDTO.java
```

用途：

```text
收支记录筛选查询参数封装
```

字段：

```java
private LocalDate startDate;
private LocalDate endDate;
private TransactionType type;
private Long behaviorTagId;
private Long emotionTagId;
private String keyword;
private Integer page;
private Integer size;
```

说明：

```text
也可以不单独创建该 DTO，直接在 Resource 方法中使用 @RequestParam。
如果筛选条件较多，建议创建该 DTO。
```

------

### 7.7 BudgetDTO

位置：

```text
src/main/java/com/everycent/service/dto/BudgetDTO.java
```

用途：

```text
预算列表
创建预算
修改预算
预算状态展示
预算进度条
预算告警判断
```

字段：

```java
private Long id;
private Long ledgerId;
private BudgetCycle cycle;
private LocalDate periodStart;
private LocalDate periodEnd;
private BigDecimal limitAmount;
private BigDecimal alertThreshold;
private Boolean enabled;
private BigDecimal usedAmount;
private BigDecimal remainingAmount;
private BigDecimal usedRatio;
```

字段来源：

| 字段              | 来源                               |
| ----------------- | ---------------------------------- |
| `id`              | `budget.id`                        |
| `ledgerId`        | `budget.ledger_id`                 |
| `cycle`           | `budget.cycle`                     |
| `periodStart`     | `budget.period_start`              |
| `periodEnd`       | `budget.period_end`                |
| `limitAmount`     | `budget.limit_amount`              |
| `alertThreshold`  | `budget.alert_threshold`           |
| `enabled`         | `budget.enabled`                   |
| `usedAmount`      | 根据 `transaction_record` 聚合计算 |
| `remainingAmount` | `limitAmount - usedAmount`         |
| `usedRatio`       | `usedAmount / limitAmount`         |

校验规则：

```text
limitAmount 必须大于 0
alertThreshold 必须在 0 到 1 之间
periodStart 必须早于或等于 periodEnd
同一账本同一周期不能重复创建预算
```

------

### 7.8 DashboardSummaryDTO

位置：

```text
src/main/java/com/everycent/service/dto/DashboardSummaryDTO.java
```

用途：

```text
数据看板总览接口响应
```

字段：

```java
private BigDecimal totalIncome;
private BigDecimal totalExpense;
private BigDecimal balance;
private Long transactionCount;
private BigDecimal budgetUsedRatio;
private AlertLevel budgetAlertLevel;
```

字段来源：

```text
totalIncome：transaction_record 聚合
totalExpense：transaction_record 聚合
balance：totalIncome - totalExpense
transactionCount：transaction_record 计数
budgetUsedRatio：BudgetService 计算
budgetAlertLevel：BudgetService 根据阈值判断
```

------

### 7.9 TrendPointDTO

位置：

```text
src/main/java/com/everycent/service/dto/TrendPointDTO.java
```

用途：

```text
数据看板收支趋势图
```

字段：

```java
private LocalDate date;
private BigDecimal income;
private BigDecimal expense;
```

------

### 7.10 TagStatDTO

位置：

```text
src/main/java/com/everycent/service/dto/TagStatDTO.java
```

用途：

```text
行为标签分类统计
情绪标签分类统计
```

字段：

```java
private Long tagId;
private String tagName;
private BigDecimal amount;
private Long count;
private BigDecimal ratio;
```

------

### 7.11 BehaviorTagDTO

位置：

```text
src/main/java/com/everycent/service/dto/BehaviorTagDTO.java
```

用途：

```text
返回行为标签列表
```

字段：

```java
private Long id;
private String code;
private String name;
private Boolean systemDefault;
```

------

### 7.12 EmotionTagDTO

位置：

```text
src/main/java/com/everycent/service/dto/EmotionTagDTO.java
```

用途：

```text
返回情绪标签列表
```

字段：

```java
private Long id;
private String code;
private String name;
private String valence;
private Boolean systemDefault;
```

------

### 7.13 NotificationMessageDTO

位置：

```text
src/main/java/com/everycent/service/dto/NotificationMessageDTO.java
```

用途：

```text
消息提醒列表
消息详情
```

字段：

```java
private Long id;
private String title;
private String content;
private NotificationType type;
private AlertLevel level;
private Boolean read;
private Instant createdDate;
private Long ledgerId;
private String ledgerName;
private Long budgetId;
```

------

## 8. Repository 设计

Repository 建议位置：

```text
src/main/java/com/everycent/repository/
```

------

### 8.1 LedgerRepository

职责：

```text
查询账本。
查询当前用户创建的账本。
配合权限表查询当前用户参与的账本。
```

建议方法：

```java
List<Ledger> findByCreatorId(Long creatorId);

Optional<Ledger> findById(Long id);
```

可选自定义查询：

```java
@Query("""
    select distinct l
    from Ledger l
    left join UserLedgerPermission p on p.ledger = l
    where l.creator.id = :userId
       or (p.user.id = :userId and p.status = com.everycent.domain.enumeration.PermissionStatus.ACTIVE)
""")
List<Ledger> findAccessibleLedgers(@Param("userId") Long userId);
```

------

### 8.2 UserLedgerPermissionRepository

职责：

```text
查询用户对账本的权限。
判断用户是否可读、可写、是否所有者。
查询账本成员。
防止重复授权。
```

建议方法：

```java
Optional<UserLedgerPermission> findByUserIdAndLedgerId(Long userId, Long ledgerId);

boolean existsByUserIdAndLedgerId(Long userId, Long ledgerId);

List<UserLedgerPermission> findByLedgerId(Long ledgerId);

List<UserLedgerPermission> findByUserId(Long userId);
```

------

### 8.3 TransactionRecordRepository

职责：

```text
查询收支记录。
按账本、日期、类型、标签筛选。
支撑预算计算、余额计算、数据看板和导出。
```

建议方法：

```java
List<TransactionRecord> findByLedgerId(Long ledgerId);

List<TransactionRecord> findByLedgerIdAndTransactionDateBetween(
    Long ledgerId,
    LocalDate startDate,
    LocalDate endDate
);
```

建议自定义统计查询：

```text
1. 查询某账本某时间范围总收入。
2. 查询某账本某时间范围总支出。
3. 查询某账本按行为标签分组支出。
4. 查询某账本按情绪标签分组支出。
5. 查询某账本按日期分组收入支出趋势。
6. 查询某预算周期内已使用金额。
```

------

### 8.4 BudgetRepository

职责：

```text
查询预算。
防止重复预算。
查询当前周期预算。
```

建议方法：

```java
List<Budget> findByLedgerId(Long ledgerId);

Optional<Budget> findByLedgerIdAndCycleAndPeriodStartAndPeriodEnd(
    Long ledgerId,
    BudgetCycle cycle,
    LocalDate periodStart,
    LocalDate periodEnd
);
```

------

### 8.5 BehaviorTagRepository

职责：

```text
查询行为标签。
根据 code 查询标签。
查询系统默认标签。
```

建议方法：

```java
Optional<BehaviorTag> findByCode(String code);

List<BehaviorTag> findBySystemDefaultTrue();
```

------

### 8.6 EmotionTagRepository

职责：

```text
查询情绪标签。
根据 code 查询标签。
查询系统默认标签。
```

建议方法：

```java
Optional<EmotionTag> findByCode(String code);

List<EmotionTag> findBySystemDefaultTrue();
```

------

### 8.7 MonthlyBalanceRepository

职责：

```text
查询和保存月度余额快照。
```

建议方法：

```java
Optional<MonthlyBalance> findByLedgerIdAndYearAndMonth(
    Long ledgerId,
    Integer year,
    Integer month
);
```

------

### 8.8 NotificationMessageRepository

职责：

```text
查询当前用户消息。
查询未读消息。
按时间倒序返回。
```

建议方法：

```java
List<NotificationMessage> findByUserIdOrderByCreatedDateDesc(Long userId);

List<NotificationMessage> findByUserIdAndReadFalseOrderByCreatedDateDesc(Long userId);
```

------

## 9. Service 设计

Service 建议位置：

```text
src/main/java/com/everycent/service/
```

------

### 9.1 LedgerPermissionService

职责：

```text
统一处理账本权限校验。
所有包含 ledgerId 的接口都必须先调用该 Service。
```

主要方法：

```java
boolean canRead(User user, Long ledgerId);

boolean canWrite(User user, Long ledgerId);

boolean isOwner(User user, Long ledgerId);

void checkReadable(User user, Long ledgerId);

void checkWritable(User user, Long ledgerId);

void checkOwner(User user, Long ledgerId);
```

权限判断规则：

| 权限       | canRead | canWrite | isOwner |
| ---------- | ------- | -------- | ------- |
| OWNER      | true    | true     | true    |
| READ_WRITE | true    | true     | false   |
| READ_ONLY  | true    | false    | false   |

异常规则：

```text
如果无读取权限，抛出 AccessDeniedException 或业务异常。
如果无写入权限，抛出 AccessDeniedException 或业务异常。
如果不是 OWNER 却执行 OWNER 操作，抛出 AccessDeniedException。
```

------

### 9.2 LedgerService

职责：

```text
账本 CRUD。
创建账本时自动创建 OWNER 权限。
查询当前用户可访问账本。
账本成员管理。
```

主要方法：

```java
LedgerDTO createLedger(LedgerDTO dto);

LedgerDTO updateLedger(Long ledgerId, LedgerDTO dto);

void deleteLedger(Long ledgerId);

LedgerDTO getLedger(Long ledgerId);

List<LedgerDTO> getCurrentUserLedgers();

List<LedgerMemberDTO> getMembers(Long ledgerId);

void inviteMember(Long ledgerId, InviteMemberRequestDTO request);

void updateMemberPermission(Long ledgerId, Long userId, UpdateMemberPermissionDTO request);

void removeMember(Long ledgerId, Long userId);
```

核心业务规则：

```text
1. 创建账本时，当前用户成为 creator。
2. 创建账本后，必须自动写入一条 OWNER 权限。
3. 只有 OWNER 可以修改账本基本信息。
4. 只有 OWNER 可以删除账本。
5. 只有 OWNER 可以邀请成员。
6. 同一用户不能重复加入同一账本。
7. 不允许移除账本 OWNER 本人，除非后续支持所有权转让。
```

------

### 9.3 TransactionRecordService

职责：

```text
手动收支记录 CRUD。
记录筛选。
新增、修改、删除记录后更新月度余额。
新增记录后检查预算状态。
```

主要方法：

```java
TransactionRecordDTO createTransaction(Long ledgerId, TransactionRecordDTO dto);

TransactionRecordDTO updateTransaction(Long transactionId, TransactionRecordDTO dto);

void deleteTransaction(Long transactionId);

TransactionRecordDTO getTransaction(Long transactionId);

List<TransactionRecordDTO> getTransactions(
    Long ledgerId,
    LocalDate startDate,
    LocalDate endDate,
    TransactionType type,
    Long behaviorTagId,
    Long emotionTagId,
    String keyword
);
```

核心业务规则：

```text
1. 新增记录前必须校验当前用户对账本有写权限。
2. 查询记录前必须校验当前用户对账本有读权限。
3. amount 必须大于 0。
4. type 必须为 INCOME 或 EXPENSE。
5. behaviorTagId 如果存在，必须能查到对应标签。
6. emotionTagId 如果存在，必须能查到对应标签。
7. 新增记录后需要更新 monthly_balance 或触发数据库触发器。
8. 新增记录后需要调用 BudgetService 计算预算状态。
9. 如果达到预算阈值，可以调用 NotificationService 生成提醒。
```

------

### 9.4 BudgetService

职责：

```text
预算 CRUD。
预算状态计算。
预算超支检测。
预算提醒触发。
```

主要方法：

```java
BudgetDTO createBudget(Long ledgerId, BudgetDTO dto);

BudgetDTO updateBudget(Long budgetId, BudgetDTO dto);

void deleteBudget(Long budgetId);

List<BudgetDTO> getBudgets(Long ledgerId);

BudgetDTO getCurrentBudgetStatus(Long ledgerId, BudgetCycle cycle);

boolean isOverBudget(Long ledgerId, BudgetCycle cycle);
```

核心业务规则：

```text
1. 创建预算前必须校验当前用户对账本有写权限。
2. 查看预算前必须校验当前用户对账本有读权限。
3. limitAmount 必须大于 0。
4. alertThreshold 必须在 0 到 1 之间。
5. periodStart 不能晚于 periodEnd。
6. 同一账本同一周期不能重复设置预算。
7. usedAmount 由收支记录中 EXPENSE 类型聚合得到。
8. remainingAmount = limitAmount - usedAmount。
9. usedRatio = usedAmount / limitAmount。
10. usedRatio >= alertThreshold 时进入预警状态。
11. usedAmount > limitAmount 时进入超支状态。
```

------

### 9.5 DashboardService

职责：

```text
为前端数据看板提供聚合统计数据。
```

主要方法：

```java
DashboardSummaryDTO getSummary(Long ledgerId, String period);

List<TrendPointDTO> getTrend(Long ledgerId, LocalDate startDate, LocalDate endDate);

List<TagStatDTO> getBehaviorTagStats(Long ledgerId, String period);

List<TagStatDTO> getEmotionTagStats(Long ledgerId, String period);
```

核心业务规则：

```text
1. 所有查询必须先校验当前用户对账本有读权限。
2. totalIncome 统计 INCOME。
3. totalExpense 统计 EXPENSE。
4. balance = totalIncome - totalExpense。
5. 行为标签统计只统计支出或按接口约定统计全部记录。
6. 情绪标签统计用于分析消费情绪分布。
7. 趋势数据按日期聚合。
```

------

### 9.6 MonthlyBalanceService

职责：

```text
维护月度余额快照。
```

主要方法：

```java
void recalculate(Long ledgerId, Integer year, Integer month);

MonthlyBalance getMonthlyBalance(Long ledgerId, Integer year, Integer month);
```

核心业务规则：

```text
1. 当新增收支记录时，重算对应月份余额。
2. 当修改收支记录时，如果日期未变，重算当前月份；如果日期变化，重算旧月份和新月份。
3. 当删除收支记录时，重算对应月份余额。
4. totalIncome = 当前月 INCOME 金额合计。
5. totalExpense = 当前月 EXPENSE 金额合计。
6. balance = totalIncome - totalExpense。
```

说明：

```text
如果数据库触发器已经实现该逻辑，则 MonthlyBalanceService 可以只提供查询方法，不负责重算。
```

------

### 9.7 NotificationService

职责：

```text
生成预算提醒、共享提醒、普通系统提醒。
查询消息列表。
标记已读。
删除消息。
```

主要方法：

```java
void createBudgetWarning(Long userId, Long ledgerId, Long budgetId, String message);

void createShareInviteMessage(Long userId, Long ledgerId, String message);

List<NotificationMessageDTO> getCurrentUserNotifications(Boolean read);

void markAsRead(Long notificationId);

void deleteNotification(Long notificationId);
```

核心业务规则：

```text
1. 用户只能查看自己的消息。
2. 用户只能标记自己的消息为已读。
3. 预算超过阈值时可以生成 WARNING 级别提醒。
4. 预算超支时可以生成 DANGER 级别提醒。
5. 账本共享邀请可以生成 SHARE_INVITE 类型消息。
```

------

### 9.8 ExcelExportService

职责：

```text
根据账本和时间范围导出收支记录 Excel。
```

主要方法：

```java
byte[] exportTransactions(Long ledgerId, LocalDate startDate, LocalDate endDate);
```

导出字段：

```text
交易日期
类型
金额
行为标签
情绪标签
描述
创建人
来源
创建时间
```

核心业务规则：

```text
1. 导出前必须校验当前用户对账本有读权限。
2. 只能导出用户有权限查看的账本。
3. 支持按 startDate 和 endDate 过滤。
4. 返回 Content-Type 为 Excel 文件类型。
```

------

## 10. Resource / REST API 设计

Resource 建议位置：

```text
src/main/java/com/everycent/web/rest/
```

------

### 10.1 LedgerResource

职责：

```text
提供账本和账本成员相关接口。
```

接口列表：

| 方法   | 路径                                       | 权限             |
| ------ | ------------------------------------------ | ---------------- |
| GET    | `/api/ledgers`                             | 登录用户         |
| POST   | `/api/ledgers`                             | 登录用户         |
| GET    | `/api/ledgers/{ledgerId}`                  | 当前用户有读权限 |
| PUT    | `/api/ledgers/{ledgerId}`                  | OWNER            |
| DELETE | `/api/ledgers/{ledgerId}`                  | OWNER            |
| GET    | `/api/ledgers/{ledgerId}/members`          | 当前用户有读权限 |
| POST   | `/api/ledgers/{ledgerId}/members`          | OWNER            |
| PUT    | `/api/ledgers/{ledgerId}/members/{userId}` | OWNER            |
| DELETE | `/api/ledgers/{ledgerId}/members/{userId}` | OWNER            |

------

### 10.2 TransactionRecordResource

职责：

```text
提供手动收支记录相关接口。
```

接口列表：

| 方法   | 路径                                   | 权限                           |
| ------ | -------------------------------------- | ------------------------------ |
| GET    | `/api/ledgers/{ledgerId}/transactions` | 当前用户有读权限               |
| POST   | `/api/ledgers/{ledgerId}/transactions` | OWNER 或 READ_WRITE            |
| GET    | `/api/transactions/{transactionId}`    | 当前用户有该记录所属账本读权限 |
| PUT    | `/api/transactions/{transactionId}`    | OWNER 或 READ_WRITE            |
| DELETE | `/api/transactions/{transactionId}`    | OWNER 或 READ_WRITE            |

说明：

```text
不要在 TransactionRecordResource 中实现自然语言解析。
自然语言解析属于 LLM 模块。
```

------

### 10.3 BudgetResource

职责：

```text
提供预算管理接口。
```

接口列表：

| 方法   | 路径                                                   | 权限                |
| ------ | ------------------------------------------------------ | ------------------- |
| GET    | `/api/ledgers/{ledgerId}/budgets`                      | 当前用户有读权限    |
| POST   | `/api/ledgers/{ledgerId}/budgets`                      | OWNER 或 READ_WRITE |
| PUT    | `/api/budgets/{budgetId}`                              | OWNER 或 READ_WRITE |
| DELETE | `/api/budgets/{budgetId}`                              | OWNER 或 READ_WRITE |
| GET    | `/api/ledgers/{ledgerId}/budgets/status?cycle=MONTHLY` | 当前用户有读权限    |

------

### 10.4 DashboardResource

职责：

```text
提供数据看板聚合统计接口。
```

接口列表：

| 方法 | 路径                                                         | 权限             |
| ---- | ------------------------------------------------------------ | ---------------- |
| GET  | `/api/ledgers/{ledgerId}/dashboard/summary?period=MONTH`     | 当前用户有读权限 |
| GET  | `/api/ledgers/{ledgerId}/dashboard/trend?startDate=...&endDate=...` | 当前用户有读权限 |
| GET  | `/api/ledgers/{ledgerId}/dashboard/behavior-tags?period=MONTH` | 当前用户有读权限 |
| GET  | `/api/ledgers/{ledgerId}/dashboard/emotion-tags?period=MONTH` | 当前用户有读权限 |

------

### 10.5 TagResource

职责：

```text
提供行为标签和情绪标签查询接口。
```

接口列表：

| 方法 | 路径                 | 权限     |
| ---- | -------------------- | -------- |
| GET  | `/api/tags/behavior` | 登录用户 |
| GET  | `/api/tags/emotion`  | 登录用户 |

------

### 10.6 ExportResource

职责：

```text
提供 Excel 导出接口。
```

接口列表：

| 方法 | 路径                                                         | 权限             |
| ---- | ------------------------------------------------------------ | ---------------- |
| GET  | `/api/ledgers/{ledgerId}/transactions/export?startDate=...&endDate=...` | 当前用户有读权限 |

响应头：

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="everycent-transactions.xlsx"
```

------

### 10.7 NotificationResource

职责：

```text
提供消息提醒接口。
```

接口列表：

| 方法   | 路径                                           | 权限           |
| ------ | ---------------------------------------------- | -------------- |
| GET    | `/api/notifications?read=false&page=0&size=20` | 登录用户       |
| PATCH  | `/api/notifications/{notificationId}/read`     | 消息接收者本人 |
| DELETE | `/api/notifications/{notificationId}`          | 消息接收者本人 |

------

## 11. Mapper 设计

如果项目采用 MapStruct，建议创建：

```text
src/main/java/com/everycent/service/mapper/
├── LedgerMapper.java
├── TransactionRecordMapper.java
├── BudgetMapper.java
├── NotificationMessageMapper.java
├── BehaviorTagMapper.java
└── EmotionTagMapper.java
```

### 11.1 Mapper 职责

Mapper 负责：

```text
Entity -> DTO
DTO -> Entity
```

但复杂字段不建议完全交给 Mapper 自动完成。

例如：

```text
currentUserPermission
memberCount
usedAmount
remainingAmount
usedRatio
behaviorTagName
emotionTagName
creatorLogin
```

这些字段需要 Service 层查询或计算后手动填充。

------

## 12. 权限校验规则

### 12.1 权限表

权限来自：

```text
user_ledger_permission
```

核心字段：

```text
user_id
ledger_id
permission_level
status
```

### 12.2 权限规则表

| 操作         | OWNER | READ_WRITE | READ_ONLY |
| ------------ | ----- | ---------- | --------- |
| 查看账本     | 是    | 是         | 是        |
| 查看记录     | 是    | 是         | 是        |
| 新增记录     | 是    | 是         | 否        |
| 修改记录     | 是    | 是         | 否        |
| 删除记录     | 是    | 是         | 否        |
| 设置预算     | 是    | 是         | 否        |
| 查看预算     | 是    | 是         | 是        |
| 查看数据看板 | 是    | 是         | 是        |
| 导出账单     | 是    | 是         | 是        |
| 邀请成员     | 是    | 否         | 否        |
| 修改成员权限 | 是    | 否         | 否        |
| 删除账本     | 是    | 否         | 否        |

### 12.3 必须调用权限校验的接口

所有包含 `ledgerId` 的接口都必须调用：

```java
ledgerPermissionService.checkReadable(currentUser, ledgerId);
```

所有写操作必须调用：

```java
ledgerPermissionService.checkWritable(currentUser, ledgerId);
```

所有账本管理操作必须调用：

```java
ledgerPermissionService.checkOwner(currentUser, ledgerId);
```

------

## 13. 数据校验规则

### 13.1 账本校验

```text
name 不允许为空
name 长度不超过 100
description 长度不超过 500
defaultCurrency 不允许为空，默认 CNY
```

### 13.2 收支记录校验

```text
amount 不允许为空
amount 必须大于 0
type 不允许为空
type 必须为 INCOME 或 EXPENSE
transactionDate 不允许为空
ledgerId 必须存在
creator 必须为当前登录用户
behaviorTagId 如果传入，必须存在
emotionTagId 如果传入，必须存在
description 长度不超过 500
```

### 13.3 预算校验

```text
limitAmount 不允许为空
limitAmount 必须大于 0
alertThreshold 不允许为空
alertThreshold 必须在 0 到 1 之间
periodStart 不允许为空
periodEnd 不允许为空
periodStart 不得晚于 periodEnd
同一账本同一周期不能重复创建预算
```

### 13.4 权限校验

```text
不能重复邀请同一用户进入同一账本
不能把普通成员设置为 OWNER，除非后续支持所有权转让
不能删除账本创建者的 OWNER 权限
READ_ONLY 用户不能新增、修改、删除记录
```

------

## 14. 异常处理设计

建议使用统一异常响应。

### 14.1 常见异常类型

| 场景       | 建议异常                                                     |
| ---------- | ------------------------------------------------------------ |
| 未登录     | `Unauthorized`                                               |
| 无权限     | `AccessDeniedException`                                      |
| 资源不存在 | `BadRequestAlertException` 或 `ResponseStatusException(HttpStatus.NOT_FOUND)` |
| 参数非法   | `BadRequestAlertException`                                   |
| 重复授权   | `BadRequestAlertException`                                   |
| 重复预算   | `BadRequestAlertException`                                   |
| 金额非法   | `BadRequestAlertException`                                   |
| 标签不存在 | `BadRequestAlertException`                                   |

### 14.2 统一错误响应示例

```json
{
  "type": "https://www.everycent.com/problem/access-denied",
  "title": "Access denied",
  "status": 403,
  "detail": "当前用户没有该账本的写入权限",
  "path": "/api/ledgers/1/transactions"
}
```

------

## 15. 核心业务流程

### 15.1 创建账本流程

```text
1. 前端提交账本名称、描述、默认货币。
2. LedgerResource 接收请求。
3. LedgerService 获取当前登录用户。
4. 创建 Ledger。
5. 保存 Ledger。
6. 创建 UserLedgerPermission，permissionLevel = OWNER。
7. 返回 LedgerDTO。
```

### 15.2 邀请成员流程

```text
1. 前端提交 loginOrEmail 和 permissionLevel。
2. LedgerResource 接收请求。
3. LedgerPermissionService 校验当前用户是否 OWNER。
4. 根据 loginOrEmail 查询目标用户。
5. 检查目标用户是否已经在该账本中。
6. 写入 UserLedgerPermission。
7. NotificationService 生成共享邀请消息。
8. 返回成功结果。
```

### 15.3 手动记账流程

```text
1. 前端提交 amount、type、tag、date、description。
2. TransactionRecordResource 接收请求。
3. LedgerPermissionService 校验当前用户是否有写权限。
4. TransactionRecordService 校验金额、类型、标签、日期。
5. 创建 TransactionRecord。
6. 保存到 transaction_record。
7. 更新 monthly_balance 或等待数据库触发器更新。
8. BudgetService 计算预算使用情况。
9. 如达到阈值，NotificationService 生成预算提醒。
10. 返回 TransactionRecordDTO 和预算状态。
```

### 15.4 预算状态计算流程

```text
1. 前端请求预算状态。
2. BudgetResource 接收请求。
3. LedgerPermissionService 校验可读权限。
4. BudgetService 查询当前周期预算。
5. TransactionRecordRepository 聚合当前周期 EXPENSE 金额。
6. 计算 usedAmount、remainingAmount、usedRatio。
7. 判断 INFO、WARNING、DANGER。
8. 返回 BudgetDTO 或预算状态 DTO。
```

### 15.5 数据看板流程

```text
1. 前端请求 dashboard summary/trend/tag stats。
2. DashboardResource 接收请求。
3. LedgerPermissionService 校验可读权限。
4. DashboardService 调用 Repository 聚合查询。
5. 组装 DashboardSummaryDTO、TrendPointDTO、TagStatDTO。
6. 返回前端图表数据。
```

------

## 16. 数据库读写关系

### 16.1 账本模块

| 操作         | 读表                                           | 写表                                             |
| ------------ | ---------------------------------------------- | ------------------------------------------------ |
| 创建账本     | `jhi_user`                                     | `ledger`, `user_ledger_permission`               |
| 查询账本列表 | `ledger`, `user_ledger_permission`             | 无                                               |
| 修改账本     | `ledger`, `user_ledger_permission`             | `ledger`                                         |
| 删除账本     | `ledger`, `user_ledger_permission`             | `ledger` 及关联处理                              |
| 查询成员     | `jhi_user`, `user_ledger_permission`           | 无                                               |
| 邀请成员     | `jhi_user`, `ledger`, `user_ledger_permission` | `user_ledger_permission`, `notification_message` |

### 16.2 收支记录模块

| 操作     | 读表                                                         | 写表                                                         |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 新增记录 | `ledger`, `user_ledger_permission`, `behavior_tag`, `emotion_tag` | `transaction_record`, `monthly_balance`, `notification_message` |
| 查询记录 | `transaction_record`, `behavior_tag`, `emotion_tag`, `jhi_user` | 无                                                           |
| 修改记录 | `transaction_record`, `user_ledger_permission`               | `transaction_record`, `monthly_balance`                      |
| 删除记录 | `transaction_record`, `user_ledger_permission`               | `transaction_record`, `monthly_balance`                      |

### 16.3 预算模块

| 操作     | 读表                               | 写表                   |
| -------- | ---------------------------------- | ---------------------- |
| 创建预算 | `ledger`, `user_ledger_permission` | `budget`               |
| 查询预算 | `budget`, `transaction_record`     | 无                     |
| 修改预算 | `budget`, `user_ledger_permission` | `budget`               |
| 删除预算 | `budget`, `user_ledger_permission` | `budget`               |
| 预算告警 | `budget`, `transaction_record`     | `notification_message` |

### 16.4 数据看板模块

| 操作         | 读表                                              | 写表 |
| ------------ | ------------------------------------------------- | ---- |
| 总览统计     | `transaction_record`, `budget`, `monthly_balance` | 无   |
| 收支趋势     | `transaction_record`                              | 无   |
| 行为标签统计 | `transaction_record`, `behavior_tag`              | 无   |
| 情绪标签统计 | `transaction_record`, `emotion_tag`               | 无   |

### 16.5 通知模块

| 操作     | 读表                   | 写表                   |
| -------- | ---------------------- | ---------------------- |
| 查询消息 | `notification_message` | 无                     |
| 标记已读 | `notification_message` | `notification_message` |
| 删除消息 | `notification_message` | `notification_message` |

------

## 17. 开发顺序建议

建议后端同学按以下顺序实现：

```text
1. 确认 Domain 与数据库表一致。
2. 实现枚举类。
3. 实现 Repository。
4. 实现 DTO。
5. 实现 LedgerPermissionService。
6. 实现 LedgerService 和 LedgerResource。
7. 实现 TagResource。
8. 实现 TransactionRecordService 和 TransactionRecordResource。
9. 实现 BudgetService 和 BudgetResource。
10. 实现 MonthlyBalanceService。
11. 实现 DashboardService 和 DashboardResource。
12. 实现 NotificationService 和 NotificationResource。
13. 实现 ExcelExportService 和 ExportResource。
14. 与前端进行接口联调。
15. 最后再与 LLM 模块联调自然语言记账。
```

当前阶段优先完成：

```text
账本 -> 权限 -> 手动记账 -> 预算 -> 看板
```

不要一开始就接 AI。

------

## 18. 测试方案

### 18.1 Service 单元测试

需要测试：

```text
1. 创建账本后是否自动创建 OWNER 权限。
2. READ_ONLY 用户是否不能创建收支记录。
3. READ_WRITE 用户是否可以创建收支记录。
4. OWNER 是否可以邀请成员。
5. 非 OWNER 是否不能邀请成员。
6. 收支记录金额为负数时是否被拒绝。
7. 不存在的 behaviorTagId 是否被拒绝。
8. 不存在的 emotionTagId 是否被拒绝。
9. 重复预算是否被拒绝。
10. 预算使用比例是否计算正确。
11. 月度余额是否计算正确。
12. 当前用户是否只能看到自己的消息。
```

### 18.2 Resource 接口测试

需要测试：

```text
GET /api/ledgers
POST /api/ledgers
GET /api/ledgers/{ledgerId}
PUT /api/ledgers/{ledgerId}
DELETE /api/ledgers/{ledgerId}

GET /api/ledgers/{ledgerId}/members
POST /api/ledgers/{ledgerId}/members
PUT /api/ledgers/{ledgerId}/members/{userId}
DELETE /api/ledgers/{ledgerId}/members/{userId}

GET /api/ledgers/{ledgerId}/transactions
POST /api/ledgers/{ledgerId}/transactions
PUT /api/transactions/{transactionId}
DELETE /api/transactions/{transactionId}

GET /api/ledgers/{ledgerId}/budgets
POST /api/ledgers/{ledgerId}/budgets
GET /api/ledgers/{ledgerId}/budgets/status

GET /api/ledgers/{ledgerId}/dashboard/summary
GET /api/ledgers/{ledgerId}/dashboard/trend
GET /api/ledgers/{ledgerId}/dashboard/behavior-tags
GET /api/ledgers/{ledgerId}/dashboard/emotion-tags

GET /api/tags/behavior
GET /api/tags/emotion

GET /api/notifications
PATCH /api/notifications/{notificationId}/read
DELETE /api/notifications/{notificationId}
```

### 18.3 权限测试

| 测试场景            | 预期结果 |
| ------------------- | -------- |
| 未登录访问业务接口  | 401      |
| 无账本权限访问账本  | 403      |
| READ_ONLY 新增记录  | 403      |
| READ_WRITE 新增记录 | 成功     |
| READ_WRITE 邀请成员 | 403      |
| OWNER 邀请成员      | 成功     |
| OWNER 删除账本      | 成功     |
| READ_ONLY 导出账单  | 成功     |
| READ_ONLY 查看看板  | 成功     |

------

## 19. 后端与前端接口交接说明

后端需要向前端明确：

```text
1. 每个接口路径。
2. 请求方法。
3. 请求参数。
4. 请求体 DTO。
5. 响应 DTO。
6. 权限要求。
7. 错误码。
8. 日期格式。
9. 金额格式。
10. 分页格式。
```

统一约定：

```text
日期：YYYY-MM-DD
时间：ISO-8601
金额：BigDecimal，JSON 中建议传字符串
分页：使用 JHipster 默认分页格式或统一 content + totalElements
认证：JWT Bearer Token
```

------

## 20. 与数据库负责人交接内容

后端开发前需要确认：

```text
1. 实际表名是否与文档一致。
2. 实际字段名是否与 Domain 一致。
3. 外键是否已经建立。
4. 唯一约束是否已经建立。
5. 默认行为标签是否已经初始化。
6. 默认情绪标签是否已经初始化。
7. monthly_balance 是由触发器维护，还是由后端 Service 维护。
8. 删除账本时关联数据如何处理。
9. MySQL 连接配置是否统一。
```

------

## 21. 与 LLM 模块交接内容

常规后端与 LLM 模块只需要约定：

```text
1. LLM 模块最终输出哪些字段。
2. 这些字段如何转换为 TransactionRecordDTO。
3. 普通后端是否复用 createTransaction 方法写库。
4. 低置信度时是否要求用户确认。
5. AI 解析失败时是否回退到手动记账。
```

当前阶段不实现：

```text
LlmClient
PromptBuilder
LlmJsonResponseParser
TransactionParseResultDTO
AiAlertResult
```

------

## 22. 最终交付清单

后端完成后，应至少交付：

```text
1. Domain 类
2. Enumeration 枚举类
3. DTO 类
4. Repository 接口
5. Service 类
6. Resource 类
7. Mapper 类
8. 权限校验逻辑
9. 参数校验逻辑
10. 预算计算逻辑
11. 月度余额计算逻辑
12. Dashboard 聚合查询逻辑
13. Excel 导出逻辑
14. 通知消息逻辑
15. 接口测试结果
16. 权限测试结果
```

------

## 23. 总结

EveryCent 常规后端模块的核心目标是形成一个完整的普通记账业务闭环：

```text
登录用户
    ↓
创建账本
    ↓
分配账本权限
    ↓
手动新增收支记录
    ↓
更新余额
    ↓
检查预算
    ↓
生成提醒
    ↓
提供数据看板
    ↓
支持账单导出
```

在这个闭环完成之前，不建议优先接入 AI。AI 模块应作为增强能力，在普通记账、权限、预算和看板稳定后再接入。

常规后端必须保证：

```text
1. 所有账本级接口都有权限校验。
2. 所有写入数据库的数据都经过合法性校验。
3. DTO 与 Entity 边界清晰。
4. 普通业务 DTO 与 AI DTO 分离。
5. 业务逻辑集中在 Service 层。
6. Resource 层只负责接收请求和返回响应。
7. Repository 层只负责数据库查询。
8. MySQL 中的数据完整性与后端校验共同保证系统可靠性。
```