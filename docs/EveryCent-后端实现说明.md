# EveryCent 后端实现说明

本文档记录当前 EveryCent 常规后端业务模块的实际实现情况，用于系统介绍、答辩说明和后续维护。内容按包结构、DTO、Service、API 接口和测试覆盖整理。

## 1. 后端包结构

后端主要代码位于：

```text
src/main/java/com/everycent/
```

核心包结构如下：

```text
com.everycent.domain
com.everycent.domain.enumeration
com.everycent.repository
com.everycent.repository.projection
com.everycent.service
com.everycent.service.dto
com.everycent.web.rest
com.everycent.web.rest.errors
```

### 1.1 domain

领域实体位于 `src/main/java/com/everycent/domain/`。

主要实体：

| 文件 | 说明 |
| --- | --- |
| `Ledger.java` | 账本实体，保存账本名称、描述、默认货币、创建人、当前月余额等信息。 |
| `UserLedgerPermission.java` | 用户账本权限实体，表示用户对某账本的 OWNER、READ_WRITE、READ_ONLY 权限。 |
| `TransactionRecord.java` | 交易记录实体，保存收入/支出金额、日期、标签、来源、创建人等。 |
| `Budget.java` | 预算实体，保存预算周期、起止日期、预算金额、提醒阈值、启用状态。 |
| `BehaviorTag.java` | 行为标签实体，例如 Food、Transport 等。 |
| `EmotionTag.java` | 情绪标签实体，例如 Happy、Stress 等。 |
| `MonthlyBalance.java` | 月度余额快照实体，保存某账本某年月收入、支出和余额。 |
| `NotificationMessage.java` | 通知消息实体，保存预算提醒、共享邀请、普通提醒等消息。 |

### 1.2 enumeration

枚举位于 `src/main/java/com/everycent/domain/enumeration/`。

| 文件 | 说明 |
| --- | --- |
| `PermissionLevel.java` | 账本权限级别：OWNER、READ_WRITE、READ_ONLY。 |
| `PermissionStatus.java` | 权限状态：ACTIVE、REVOKED。 |
| `TransactionType.java` | 交易类型：INCOME、EXPENSE。 |
| `RecordSource.java` | 记录来源：MANUAL、AI 等。 |
| `BudgetCycle.java` | 预算周期：MONTHLY、WEEKLY 等。 |
| `NotificationType.java` | 通知类型：BUDGET_ALERT、REMINDER、SHARE_INVITE。 |
| `NotificationLevel.java` | 通知级别：INFO、WARNING、DANGER。 |
| `EmotionValence.java` | 情绪倾向。 |

### 1.3 repository

Repository 位于 `src/main/java/com/everycent/repository/`。

主要 Repository：

| 文件 | 说明 |
| --- | --- |
| `LedgerRepository.java` | 查询用户可访问账本、按名称查重。 |
| `UserLedgerPermissionRepository.java` | 查询、保存、撤销账本成员权限。 |
| `TransactionRecordRepository.java` | 查询交易列表、按日期范围统计收入支出、按标签统计支出、导出查询。 |
| `BudgetRepository.java` | 查询账本预算、启用预算、周期预算查重。 |
| `BehaviorTagRepository.java` | 查询行为标签。 |
| `EmotionTagRepository.java` | 查询情绪标签。 |
| `MonthlyBalanceRepository.java` | 查询和保存月度余额快照。 |
| `NotificationMessageRepository.java` | 查询通知、分页筛选、标记/删除、预算通知查重。 |

聚合查询投影位于 `src/main/java/com/everycent/repository/projection/`：

| 文件 | 说明 |
| --- | --- |
| `DateAmountProjection.java` | Dashboard 趋势按日期聚合收入/支出。 |
| `TagAmountProjection.java` | Dashboard 标签统计聚合金额和笔数。 |

## 2. DTO 设计

DTO 位于：

```text
src/main/java/com/everycent/service/dto/
```

### 2.1 账本 DTO

| 文件 | 说明 |
| --- | --- |
| `LedgerCreateDTO.java` | 创建账本请求，包含 `name`、`description`。 |
| `LedgerUpdateDTO.java` | 修改账本请求，包含 `name`、`description`。 |
| `LedgerDTO.java` | 账本响应，包含账本信息、创建人、当前用户权限。 |
| `LedgerMemberRequestDTO.java` | 添加成员请求，包含 `userId`、`permissionLevel/permissionType`。 |
| `LedgerMemberUpdateDTO.java` | 修改成员权限请求。 |
| `LedgerMemberDTO.java` | 成员响应，包含账本 ID、用户 ID、登录名、权限、状态。 |

实现要点：

- 创建账本时自动创建 OWNER 权限。
- 账本名称不可重复，重复时返回 `error.ledgernameexists`。
- 添加成员时不允许重复添加 ACTIVE 成员。
- 被撤销成员再次添加时会恢复权限。
- 添加或恢复成员时会生成共享邀请通知。

### 2.2 交易 DTO

| 文件 | 说明 |
| --- | --- |
| `TransactionRecordDTO.java` | 交易记录请求/响应 DTO。 |
| `TransactionQueryDTO.java` | 交易列表查询条件。 |
| `TransactionPageDTO.java` | 交易分页响应，包含 `content`、`totalElements`、`page`、`size`。 |

实现要点：

- 金额使用 `BigDecimal`，JSON 中按字符串兼容输出。
- 支持 `recordDate` 和 `transactionDate` 兼容。
- 支持行为标签和情绪标签 ID/名称返回。
- 列表接口支持分页、类型、日期范围、行为标签、情绪标签筛选。
- 日期范围错误返回 `error.invaliddaterange`。
- 创建、修改、删除交易后会触发月度余额重算和预算提醒检查。

### 2.3 预算 DTO

| 文件 | 说明 |
| --- | --- |
| `BudgetDTO.java` | 预算请求/响应 DTO。 |

实现要点：

- 创建预算时支持 `amount` 作为 `limitAmount` 的兼容字段。
- 同一账本、同一周期、同一起止日期不能重复创建预算。
- 预算状态返回使用金额、剩余金额、使用比例、状态等信息。
- 兼容字段包括 `amount`、`budgetAmount`、`usageRate`、`overBudget`。

### 2.4 标签和 Dashboard DTO

| 文件 | 说明 |
| --- | --- |
| `BehaviorTagDTO.java` | 行为标签响应。 |
| `EmotionTagDTO.java` | 情绪标签响应。 |
| `DashboardSummaryDTO.java` | Dashboard 汇总数据。 |
| `TrendPointDTO.java` | 收支趋势点。 |
| `TagStatDTO.java` | 标签统计数据。 |

实现要点：

- Dashboard 汇总返回收入、支出、余额、交易数、预算使用率和预算告警级别。
- 为适配接口契约，汇总字段同时支持 `totalIncome/incomeTotal`、`totalExpense/expenseTotal`、`budgetUsedRatio/budgetUsedRate`。
- 标签统计返回 `ratio`，同时提供兼容字段 `percentage`。
- Dashboard period 支持重复参数兼容，例如 `period=MONTH&period=MONTH`。

### 2.5 通知 DTO

| 文件 | 说明 |
| --- | --- |
| `NotificationMessageDTO.java` | 通知消息响应 DTO。 |
| `NotificationPageDTO.java` | 通知分页响应。 |

实现要点：

- 通知分页结构为 `content`、`totalElements`、`page`、`size`。
- 通知包含 `id`、`userId`、`ledgerId`、`budgetId`、`title`、`content`、`type`、`level`、`read`、`createdDate`。
- 通知只能由接收者本人查询、标记已读和删除。

## 3. Service 实现

Service 位于：

```text
src/main/java/com/everycent/service/
```

### 3.1 LedgerPermissionService

文件：`LedgerPermissionService.java`

职责：

- 统一处理账本权限校验。
- 校验读权限、写权限、OWNER 权限。
- 提供 `getLedgerOrThrow`、`getActivePermission` 等公共方法。

### 3.2 LedgerService

文件：`LedgerService.java`

职责：

- 创建、查询、修改、删除账本。
- 查询、添加、修改、删除账本成员。
- 创建账本时自动生成 OWNER 权限。
- 删除账本前清理通知、交易、月度余额、预算、成员权限，避免外键错误。
- 添加成员成功后调用 `NotificationService` 生成 `SHARE_INVITE` 通知。

关键规则：

- 账本名称不能重复。
- 普通成员不能被设置为 OWNER。
- OWNER 成员不能通过普通删除成员接口移除。
- READ_ONLY 用户不能新增、修改、删除交易。

### 3.3 TransactionRecordService

文件：`TransactionRecordService.java`

职责：

- 创建、查询、修改、删除交易记录。
- 查询账本交易分页列表。
- 校验交易所属账本不可变。
- 解析和校验行为标签、情绪标签。
- 创建/修改/删除后调用：
  - `MonthlyBalanceService.recalculate`
  - `BudgetAlertService.checkBudgetAlerts`

关键规则：

- 交易日期范围查询中 `startDate > endDate` 返回 400。
- 修改交易跨月份时，会同时重算旧月份和新月份。

### 3.4 BudgetService

文件：`BudgetService.java`

职责：

- 查询账本预算列表。
- 创建、修改、删除预算。
- 查询预算状态。
- 计算预算已使用金额、剩余金额、使用比例和状态。

预算状态：

| 状态 | 条件 |
| --- | --- |
| INFO | 使用比例低于提醒阈值。 |
| WARNING | 使用比例达到提醒阈值。 |
| DANGER | 支出金额超过预算金额。 |

### 3.5 BudgetAlertService

文件：`BudgetAlertService.java`

职责：

- 在交易变更后检查启用中的预算。
- 如果支出达到阈值，生成 `BUDGET_ALERT` + `WARNING` 通知。
- 如果支出超过预算，生成 `BUDGET_ALERT` + `DANGER` 通知。
- 同一用户、同一预算、同一级别已有未读提醒时，不重复生成。

### 3.6 MonthlyBalanceService

文件：`MonthlyBalanceService.java`

职责：

- 按账本和月份重算月度余额快照。
- 汇总当月 INCOME、EXPENSE。
- 保存或更新 `monthly_balance`。

说明：

- 该模块无独立 Resource。
- 由交易新增、修改、删除流程自动触发。

### 3.7 NotificationService

文件：`NotificationService.java`

职责：

- 查询当前用户通知。
- 支持按 `read` 状态筛选。
- 标记通知已读。
- 删除通知。
- 提供通知创建方法，供预算提醒和共享邀请复用。

### 3.8 TagService

文件：`TagService.java`

职责：

- 查询行为标签列表。
- 查询情绪标签列表。

### 3.9 DashboardService

文件：`DashboardService.java`

职责：

- Dashboard 汇总。
- 收支趋势。
- 行为标签支出统计。
- 情绪标签支出统计。

支持 period：

- `MONTH`
- `WEEK`
- `YEAR`

### 3.10 ExcelExportService

文件：`ExcelExportService.java`

职责：

- 校验当前用户对账本有读权限。
- 校验导出日期范围。
- 查询日期范围内交易记录。
- 使用 Apache POI 生成 `.xlsx` 文件。

导出列：

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

## 4. API 接口实现

Resource 位于：

```text
src/main/java/com/everycent/web/rest/
```

### 4.1 LedgerResource

文件：`LedgerResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ledgers` | 查询当前用户可访问账本。 |
| POST | `/api/ledgers` | 创建账本。 |
| GET | `/api/ledgers/{ledgerId}` | 查询账本详情。 |
| PUT | `/api/ledgers/{ledgerId}` | 修改账本。 |
| DELETE | `/api/ledgers/{ledgerId}` | 删除账本。 |
| GET | `/api/ledgers/{ledgerId}/members` | 查询账本成员。 |
| POST | `/api/ledgers/{ledgerId}/members` | 添加账本成员。 |
| PUT | `/api/ledgers/{ledgerId}/members/{userId}` | 修改成员权限。 |
| DELETE | `/api/ledgers/{ledgerId}/members/{userId}` | 移除成员。 |

### 4.2 TransactionRecordResource

文件：`TransactionRecordResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ledgers/{ledgerId}/transactions` | 查询交易分页列表。 |
| POST | `/api/ledgers/{ledgerId}/transactions` | 创建交易。 |
| POST | `/api/ledgers/{ledgerId}/transactions/natural-language` | 自然语言记账入口。 |
| GET | `/api/transactions/{transactionId}` | 查询交易详情。 |
| PUT | `/api/transactions/{transactionId}` | 修改交易。 |
| DELETE | `/api/transactions/{transactionId}` | 删除交易。 |

查询参数：

```text
page
size
type
startDate
endDate
behaviorTagId
emotionTagId
```

### 4.3 BudgetResource

文件：`BudgetResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ledgers/{ledgerId}/budgets` | 查询账本预算列表。 |
| POST | `/api/ledgers/{ledgerId}/budgets` | 创建预算。 |
| PUT | `/api/budgets/{budgetId}` | 修改预算。 |
| DELETE | `/api/budgets/{budgetId}` | 删除预算。 |
| GET | `/api/ledgers/{ledgerId}/budgets/status` | 查询预算状态。 |

预算状态查询参数：

```text
cycle
date
```

### 4.4 TagResource

文件：`TagResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/tags/behavior` | 查询行为标签。 |
| GET | `/api/tags/emotion` | 查询情绪标签。 |

### 4.5 DashboardResource

文件：`DashboardResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ledgers/{ledgerId}/dashboard/summary` | 查询看板汇总。 |
| GET | `/api/ledgers/{ledgerId}/dashboard/trend` | 查询收支趋势。 |
| GET | `/api/ledgers/{ledgerId}/dashboard/behavior-tags` | 查询行为标签统计。 |
| GET | `/api/ledgers/{ledgerId}/dashboard/emotion-tags` | 查询情绪标签统计。 |

### 4.6 NotificationResource

文件：`NotificationResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/notifications` | 查询当前用户通知。 |
| PATCH | `/api/notifications/{notificationId}/read` | 标记通知为已读。 |
| DELETE | `/api/notifications/{notificationId}` | 删除通知。 |

查询参数：

```text
read
page
size
```

说明：

- 未登录访问返回 401，通常无 JSON Body。
- 通知操作必须使用通知接收者本人 token。

### 4.7 ExportResource

文件：`ExportResource.java`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ledgers/{ledgerId}/transactions/export` | 导出账本交易 Excel。 |

查询参数：

```text
startDate
endDate
```

响应：

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="everycent-transactions.xlsx"
```

说明：

- 该接口返回文件流，不是 JSON。
- Apifox 契约应设置为 binary/file 响应。

## 5. 关键业务流程

### 5.1 创建账本

```text
LedgerResource -> LedgerService -> LedgerRepository
                            -> UserLedgerPermissionRepository
```

流程：

1. 校验账本名称是否重复。
2. 保存 Ledger。
3. 自动创建 OWNER 权限。
4. 返回 LedgerDTO。

### 5.2 邀请成员

```text
LedgerResource -> LedgerService -> UserLedgerPermissionRepository
                            -> NotificationService
```

流程：

1. 校验当前用户是 OWNER。
2. 查询目标用户。
3. 校验是否已有 ACTIVE 权限。
4. 创建或恢复成员权限。
5. 给被邀请用户生成 `SHARE_INVITE` 通知。

### 5.3 手动记账

```text
TransactionRecordResource -> TransactionRecordService
                         -> TransactionRecordRepository
                         -> MonthlyBalanceService
                         -> BudgetAlertService
```

流程：

1. 校验写权限。
2. 校验标签。
3. 保存交易。
4. 重算月度余额。
5. 检查预算阈值并必要时生成通知。
6. 返回 TransactionRecordDTO。

### 5.4 预算提醒

```text
TransactionRecordService -> BudgetAlertService
                        -> BudgetRepository
                        -> TransactionRecordRepository
                        -> NotificationService
```

流程：

1. 查找交易日期所在的启用预算。
2. 汇总预算周期内 EXPENSE 金额。
3. 判断 INFO、WARNING、DANGER。
4. WARNING/DANGER 时生成预算通知。
5. 已有同级别未读通知时跳过，避免刷屏。

### 5.5 Excel 导出

```text
ExportResource -> ExcelExportService
              -> LedgerPermissionService
              -> TransactionRecordRepository
              -> Apache POI
```

流程：

1. 校验日期范围。
2. 校验读权限。
3. 查询交易记录。
4. 生成 `.xlsx` 文件。
5. 以附件形式返回。

## 6. 测试覆盖

主要单元测试位于：

```text
src/test/java/com/everycent/service/
```

| 文件 | 覆盖内容 |
| --- | --- |
| `LedgerServiceTest.java` | 账本创建、重复名称、成员添加/恢复/删除、级联删除。 |
| `LedgerPermissionServiceTest.java` | 权限校验。 |
| `TransactionRecordServiceTest.java` | 交易创建、查询、修改、删除、余额重算、预算提醒触发。 |
| `BudgetServiceTest.java` | 预算创建、查重、状态计算、删除。 |
| `BudgetAlertServiceTest.java` | 预算 WARNING/DANGER 通知和重复提醒跳过。 |
| `MonthlyBalanceServiceTest.java` | 月度余额快照创建和更新。 |
| `NotificationServiceTest.java` | 通知分页、标记已读、禁止操作他人通知。 |
| `DashboardServiceTest.java` | Dashboard 汇总、趋势、标签统计和参数兼容。 |
| `TagServiceTest.java` | 行为标签和情绪标签查询。 |
| `ExcelExportServiceTest.java` | Excel 文件生成、内容校验、日期范围异常。 |

近期核心服务回归测试覆盖：

```text
BudgetAlertServiceTest
BudgetServiceTest
ExcelExportServiceTest
LedgerServiceTest
MonthlyBalanceServiceTest
NotificationServiceTest
TransactionRecordServiceTest
```

示例结果：

```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 7. Apifox 测试注意事项

### 7.1 路径变量

Apifox 中应使用：

```text
{{ledgerId}}
{{transactionId}}
{{budgetId}}
{{notificationId}}
```

不要使用：

```text
{ledgerId}
undefined
```

### 7.2 Token 使用

常规接口需要：

```text
Authorization: Bearer {{token}}
```

共享邀请通知需要注意：

```text
admin token：邀请成员
user token：查询被邀请用户通知
```

### 7.3 文件接口

Excel 导出接口不是 JSON 响应。

Apifox 成功响应应配置为：

```text
200
binary/file
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

### 7.4 常见错误含义

| 现象 | 含义 |
| --- | --- |
| 401 Unauthorized | 未登录或 token 无效。 |
| `No static resource api/...` | 当前后端没有加载对应 Resource，通常需要重启或确认访问地址。 |
| `undefined` 路径变量 | Apifox 环境变量没有提取成功。 |
| `error.ledgernameexists` | 账本名称重复。 |
| `error.budgetexists` | 同周期预算重复。 |
| `error.permissionexists` | 成员已在账本中。 |
| `error.invaliddaterange` | 开始日期晚于结束日期。 |

## 8. 当前完成情况

已完成的常规后端模块：

```text
账本管理
成员共享
权限校验
交易记录
标签查询
预算管理
预算提醒
通知查询/已读/删除
Dashboard 汇总/趋势/标签统计
月度余额快照
Excel 导出
```

可选增强项：

```text
定时记账提醒
异步导出任务
更完整的前端页面对接
接口级自动化回归测试
```
