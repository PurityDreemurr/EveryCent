# EveryCent Skill 化后端 Action 协议文档

## 1. 目标

本文档用于指导 EveryCent AI 助手通过结构化 action 自动调用后端能力，帮助用户完成账本系统中的可操作任务。

当前项目已经具备账本、成员、收支记录、预算、看板统计、通知、导出、标签、自然语言解析、预算提醒文案生成等后端接口。Skill 设计必须与这些真实接口和 DTO 字段保持一致，不引入当前项目不存在的服务名、接口路径或字段。

目标流程：

```text
User Input
  ↓
LLM Planner / Rule Router
  ↓
Action JSON
  ↓
Schema Validator
  ↓
SkillRouter
  ↓
SkillExecutor
  ↓
调用现有 Service / REST 等价能力
  ↓
SkillResult
  ↓
ResponseRenderer
  ↓
Final Reply
```

核心原则：

1. LLM 只输出结构化 action，不直接执行功能。
2. 后端只允许执行白名单 action，不能让 LLM 指定任意类名、方法名、SQL 或 URL。
3. 所有读写操作都必须使用当前登录用户鉴权。
4. 读操作可以自动执行；高风险写操作必须二次确认。
5. 所有 action 参数必须映射到项目现有 DTO 或服务方法。
6. Renderer 才负责生成最终用户可见回复。

---

## 2. 当前项目真实后端能力

### 2.1 认证与账号

| 能力 | 接口 | 说明 | 是否建议开放给 Skill |
|---|---|---|---|
| 登录 | `POST /api/authenticate` | 获取 JWT | 否，由前端/客户端处理 |
| 获取当前账号 | `GET /api/account` | 当前登录用户信息 | 可只读调用 |
| 修改账号信息 | `POST /api/account` | 更新当前用户资料 | 高风险，需确认 |
| 修改密码 | `POST /api/account/change-password` | 修改密码 | 不建议由 LLM 自动调用 |
| 注册/激活/重置密码 | `/api/register` 等 | 账号生命周期 | 不建议由 LLM 自动调用 |

账号类能力不是账本核心功能，默认不作为 Planner 首批工具。

### 2.2 账本 Ledger

来源：`LedgerResource`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `ledger.list` | `GET /api/ledgers` | `LedgerService.findLedgersForUser` | 登录用户 |
| `ledger.get` | `GET /api/ledgers/{ledgerId}` | `LedgerService.findOne` | 读权限 |
| `ledger.create` | `POST /api/ledgers` | `LedgerService.createLedger` | 登录用户 |
| `ledger.update` | `PUT /api/ledgers/{ledgerId}` | `LedgerService.updateLedger` | 写/Owner 权限 |
| `ledger.delete` | `DELETE /api/ledgers/{ledgerId}` | `LedgerService.deleteLedger` | Owner 权限，必须确认 |

`ledger.create` 参数映射 `LedgerCreateDTO`：

```json
{
  "name": "日常账本",
  "description": "用于记录日常收支"
}
```

`ledger.update` 参数映射 `LedgerUpdateDTO`：

```json
{
  "ledgerId": 1,
  "name": "新的账本名",
  "description": "新的说明"
}
```

### 2.3 账本成员 Ledger Members

来源：`LedgerResource`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `ledger.member.list` | `GET /api/ledgers/{ledgerId}/members` | `LedgerService.findMembers` | 读权限 |
| `ledger.member.add` | `POST /api/ledgers/{ledgerId}/members` | `LedgerService.addMember` | Owner 权限，必须确认 |
| `ledger.member.update` | `PUT /api/ledgers/{ledgerId}/members/{userId}` | `LedgerService.updateMember` | Owner 权限，必须确认 |
| `ledger.member.remove` | `DELETE /api/ledgers/{ledgerId}/members/{userId}` | `LedgerService.removeMember` | Owner 权限，必须确认 |

成员新增参数映射 `LedgerMemberRequestDTO`：

```json
{
  "ledgerId": 1,
  "userId": 2,
  "permissionLevel": "READ_WRITE"
}
```

成员权限更新参数映射 `LedgerMemberUpdateDTO`：

```json
{
  "ledgerId": 1,
  "userId": 2,
  "permissionLevel": "READ_ONLY"
}
```

`permissionLevel` 必须使用项目枚举 `PermissionLevel` 的真实值，例如 `OWNER`、`READ_WRITE`、`READ_ONLY`。

### 2.4 收支记录 Transactions

来源：`TransactionRecordResource`、`TransactionRecordService`、`LlmParsingService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `transaction.list` | `GET /api/ledgers/{ledgerId}/transactions` | `TransactionRecordService.findByLedger` | 读权限 |
| `transaction.get` | `GET /api/transactions/{transactionId}` | `TransactionRecordService.findOne` | 读权限 |
| `transaction.create` | `POST /api/ledgers/{ledgerId}/transactions` | `TransactionRecordService.create` | 写权限 |
| `transaction.create_from_text` | `POST /api/ledgers/{ledgerId}/transactions/natural-language` | `LlmParsingService.parseAndCreateTransaction` | 写权限，`confirm=true` |
| `transaction.update` | `PUT /api/transactions/{transactionId}` | `TransactionRecordService.update` | 写权限，必须确认 |
| `transaction.delete` | `DELETE /api/transactions/{transactionId}` | `TransactionRecordService.delete` | 写权限，必须确认 |
| `transaction.parse` | `POST /api/ai/transaction/parse` | `LlmParsingService.parseTransaction` | 写权限校验 |

查询参数映射 `TransactionQueryDTO`：

```json
{
  "ledgerId": 1,
  "page": 0,
  "size": 20,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "type": "EXPENSE",
  "behaviorTagId": 1,
  "emotionTagId": 2
}
```

创建/更新参数映射 `TransactionRecordDTO`：

```json
{
  "ledgerId": 1,
  "amount": "28.00",
  "type": "EXPENSE",
  "behaviorTagId": 1,
  "emotionTagId": 2,
  "recordDate": "2026-06-18",
  "description": "外卖",
  "source": "AI",
  "rawInput": "外卖花了28"
}
```

注意：

1. `amount` 建议按字符串传递，避免浮点精度问题。
2. 日期字段对外 JSON 使用 `recordDate`，DTO 也兼容 `transactionDate`。
3. `type` 必须使用项目枚举 `TransactionType`：`EXPENSE` 或 `INCOME`。
4. `source` 必须使用项目枚举 `RecordSource` 的真实值；若无法确定，可由后端默认。
5. `transaction.create_from_text` 的请求体必须包含 `confirm=true`，否则服务会拒绝入库。

自然语言创建参数映射 `NaturalLanguageTransactionCreateRequestDTO`：

```json
{
  "ledgerId": 1,
  "text": "午饭 28，地铁 6",
  "transactionDate": "2026-06-18",
  "confirm": true
}
```

自然语言解析参数映射 `TransactionParseRequestDTO`：

```json
{
  "ledgerId": 1,
  "text": "午饭 28，地铁 6",
  "transactionDate": "2026-06-18"
}
```

### 2.5 预算 Budgets

来源：`BudgetResource`、`BudgetService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `budget.list` | `GET /api/ledgers/{ledgerId}/budgets` | `BudgetService.findByLedger` | 读权限 |
| `budget.create` | `POST /api/ledgers/{ledgerId}/budgets` | `BudgetService.create` | 写权限 |
| `budget.update` | `PUT /api/budgets/{budgetId}` | `BudgetService.update` | 写权限，建议确认 |
| `budget.delete` | `DELETE /api/budgets/{budgetId}` | `BudgetService.delete` | 写权限，必须确认 |
| `budget.status` | `GET /api/ledgers/{ledgerId}/budgets/status` | `BudgetService.getStatus` | 读权限 |

预算创建/更新参数映射 `BudgetDTO`：

```json
{
  "ledgerId": 1,
  "cycle": "MONTHLY",
  "periodStart": "2026-06-01",
  "periodEnd": "2026-06-30",
  "limitAmount": "3000.00",
  "alertThreshold": "0.80",
  "enabled": true
}
```

预算状态查询参数：

```json
{
  "ledgerId": 1,
  "cycle": "MONTHLY",
  "date": "2026-06-18"
}
```

`cycle` 必须使用项目枚举 `BudgetCycle` 的真实值，例如 `WEEKLY`、`MONTHLY`。

### 2.6 预算提醒文案

来源：`LlmResource`、`LlmParsingService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `budget.alert.generate` | `POST /api/ai/budget-alert/generate` | `LlmParsingService.generateBudgetAlert` | 读权限，可选保存通知 |

参数映射 `AiAlertRequestDTO`：

```json
{
  "ledgerId": 1,
  "budgetId": 10,
  "saveAsNotification": true
}
```

注意：

1. 该能力内部会再次调用 LLM 生成提醒文案。
2. `saveAsNotification=true` 会产生通知写入行为，建议由 Planner 标记为需要确认，或由用户明确要求“保存为通知”。

### 2.7 看板 Dashboard

来源：`DashboardResource`、`DashboardService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `dashboard.summary` | `GET /api/ledgers/{ledgerId}/dashboard/summary` | `DashboardService.getSummary` | 读权限 |
| `dashboard.trend` | `GET /api/ledgers/{ledgerId}/dashboard/trend` | `DashboardService.getTrend` | 读权限 |
| `dashboard.behavior_tags` | `GET /api/ledgers/{ledgerId}/dashboard/behavior-tags` | `DashboardService.getBehaviorTagStats` | 读权限 |
| `dashboard.emotion_tags` | `GET /api/ledgers/{ledgerId}/dashboard/emotion-tags` | `DashboardService.getEmotionTagStats` | 读权限 |

汇总参数：

```json
{
  "ledgerId": 1,
  "period": "MONTH",
  "date": "2026-06-18"
}
```

趋势参数：

```json
{
  "ledgerId": 1,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30"
}
```

标签统计参数：

```json
{
  "ledgerId": 1,
  "period": "MONTH",
  "date": "2026-06-18"
}
```

`period` 支持服务中已有规则：`WEEK`、`WEEKLY`、`MONTH`、`MONTHLY`、`YEAR`、`YEARLY`。

### 2.8 标签 Tags

来源：`TagResource`、`TagService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `tag.behavior.list` | `GET /api/tags/behavior` | `TagService.findBehaviorTags` | 登录用户 |
| `tag.emotion.list` | `GET /api/tags/emotion` | `TagService.findEmotionTags` | 登录用户 |

标签只读，用于帮助 Planner/Skill 选择真实存在的 `behaviorTagId` 和 `emotionTagId`。

### 2.9 通知 Notifications

来源：`NotificationResource`、`NotificationService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `notification.list` | `GET /api/notifications` | `NotificationService.findForUser` | 当前用户 |
| `notification.mark_read` | `PATCH /api/notifications/{notificationId}/read` | `NotificationService.markAsRead` | 当前用户 |
| `notification.delete` | `DELETE /api/notifications/{notificationId}` | `NotificationService.delete` | 当前用户，建议确认 |

通知查询参数：

```json
{
  "read": false,
  "page": 0,
  "size": 20
}
```

### 2.10 导出 Export

来源：`ExportResource`、`ExcelExportService`

| Skill Action | HTTP 等价接口 | Service 等价能力 | 权限 |
|---|---|---|---|
| `export.transactions` | `GET /api/ledgers/{ledgerId}/transactions/export` | `ExcelExportService.exportTransactions` | 读权限 |

参数：

```json
{
  "ledgerId": 1,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30"
}
```

该 action 返回文件二进制或下载响应，不应把 Excel 内容塞回 LLM。Renderer 应提示导出已准备好或交给前端下载处理。

### 2.11 AI 对话入口

来源：`AiAssistantResource`

| 能力 | 接口 | 说明 |
|---|---|---|
| AI 对话 | `POST /api/assistant/chat` | 当前对话入口，未来 Skill Runtime 可挂在此流程中 |

`/api/assistant/chat` 本身不应作为 Planner 可调用工具，避免递归调用。

---

## 3. Action 命名白名单

所有可由 LLM Planner 输出的 action 必须在白名单内。

```text
ledger.list
ledger.get
ledger.create
ledger.update
ledger.delete

ledger.member.list
ledger.member.add
ledger.member.update
ledger.member.remove

transaction.list
transaction.get
transaction.parse
transaction.create
transaction.create_from_text
transaction.update
transaction.delete

budget.list
budget.create
budget.update
budget.delete
budget.status
budget.alert.generate

dashboard.summary
dashboard.trend
dashboard.behavior_tags
dashboard.emotion_tags

tag.behavior.list
tag.emotion.list

notification.list
notification.mark_read
notification.delete

export.transactions

chat.respond
chat.ask_clarification
```

不允许 Planner 输出：

1. 任意 URL。
2. 任意 Java 类名或方法名。
3. SQL、JPQL、Repository 名称。
4. 管理员接口 action。
5. 认证、改密码、注册、重置密码等账号生命周期 action。

---

## 4. Intent 与 Scene

### 4.1 AssistantIntent

建议枚举：

```java
public enum AssistantIntent {
    LEDGER_MANAGE,
    LEDGER_MEMBER_MANAGE,
    TRANSACTION_RECORD,
    TRANSACTION_QUERY,
    TRANSACTION_MODIFY,
    BUDGET_MANAGE,
    BUDGET_QUERY,
    FINANCE_ANALYSIS,
    NOTIFICATION_MANAGE,
    EXPORT_DATA,
    DAILY_CHAT,
    TASK_HELP,
    CLARIFICATION,
    UNKNOWN
}
```

### 4.2 SceneType

建议枚举：

```java
public enum SceneType {
    ACCOUNTING,
    TRANSACTION_QUERY,
    BUDGET,
    DASHBOARD_ANALYSIS,
    LEDGER,
    LEDGER_MEMBER,
    NOTIFICATION,
    EXPORT,
    TAG_LOOKUP,
    DAILY_CHAT,
    TASK_HELP,
    CLARIFICATION,
    UNKNOWN
}
```

### 4.3 DialogueAct

建议枚举：

```java
public enum DialogueAct {
    CONFIRM_CREATED,
    CONFIRM_UPDATED,
    CONFIRM_DELETED,
    SHOW_RESULT,
    SUMMARIZE_RESULT,
    ASK_MISSING_INFO,
    ASK_CONFIRMATION,
    EXPLAIN_FAILURE,
    ACKNOWLEDGE,
    NONE
}
```

---

## 5. Planner 输出 JSON 协议

Planner 必须只输出合法 JSON，不输出 Markdown，不输出自然语言说明。

统一结构：

```json
{
  "intent": "TRANSACTION_RECORD",
  "confidence": 0.95,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "transaction.create_from_text",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "text": "午饭28",
        "transactionDate": "2026-06-18",
        "confirm": true
      }
    }
  ],
  "reply_style": {
    "scene": "ACCOUNTING",
    "dialogue_act": "CONFIRM_CREATED",
    "tone": "concise",
    "mood": 45,
    "emoji": "peace",
    "tags": ["transaction"]
  }
}
```

### 5.1 缺少账本上下文

如果用户请求需要 `ledgerId`，但当前会话没有默认账本，Planner 应先调用 `ledger.list` 或请求澄清。

示例：

```json
{
  "intent": "CLARIFICATION",
  "confidence": 0.8,
  "need_user_reply": true,
  "need_confirmation": false,
  "actions": [
    {
      "name": "ledger.list",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {}
    }
  ],
  "reply_style": {
    "scene": "CLARIFICATION",
    "dialogue_act": "ASK_MISSING_INFO",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["ledger_required"]
  }
}
```

Renderer 可根据 `ledger.list` 结果让用户选择账本；如果后端已保存默认账本，也可直接补全。

### 5.2 自然语言记账

用户：

```text
午饭28，地铁6，记到账本1
```

Planner：

```json
{
  "intent": "TRANSACTION_RECORD",
  "confidence": 0.95,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "transaction.create_from_text",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "text": "午饭28，地铁6",
        "transactionDate": null,
        "confirm": true
      }
    }
  ],
  "reply_style": {
    "scene": "ACCOUNTING",
    "dialogue_act": "CONFIRM_CREATED",
    "tone": "concise",
    "mood": 45,
    "emoji": "peace",
    "tags": ["transaction", "natural_language"]
  }
}
```

### 5.3 只解析不入库

用户：

```text
帮我看看“午饭28，地铁6”能解析成什么
```

Planner：

```json
{
  "intent": "TRANSACTION_RECORD",
  "confidence": 0.9,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "transaction.parse",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "text": "午饭28，地铁6",
        "transactionDate": null
      }
    }
  ],
  "reply_style": {
    "scene": "ACCOUNTING",
    "dialogue_act": "SHOW_RESULT",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["parse_only"]
  }
}
```

### 5.4 查询账单

用户：

```text
查一下本月餐饮支出
```

Planner：

```json
{
  "intent": "TRANSACTION_QUERY",
  "confidence": 0.9,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "transaction.list",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "page": 0,
        "size": 50,
        "startDate": "2026-06-01",
        "endDate": "2026-06-30",
        "type": "EXPENSE",
        "behaviorTagId": null,
        "emotionTagId": null
      }
    }
  ],
  "reply_style": {
    "scene": "TRANSACTION_QUERY",
    "dialogue_act": "SUMMARIZE_RESULT",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["query"]
  }
}
```

如果需要按“餐饮”过滤，Planner 应优先调用 `tag.behavior.list` 获取真实标签 ID，或由 Skill 层通过标签名称解析为 ID。

### 5.5 修改账单

用户：

```text
把刚才那笔午饭改成32元
```

Planner 应先查询候选记录，再要求确认：

```json
{
  "intent": "TRANSACTION_MODIFY",
  "confidence": 0.82,
  "need_user_reply": true,
  "need_confirmation": true,
  "actions": [
    {
      "name": "transaction.list",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "page": 0,
        "size": 5,
        "startDate": "2026-06-18",
        "endDate": "2026-06-18",
        "type": "EXPENSE"
      }
    }
  ],
  "reply_style": {
    "scene": "ACCOUNTING",
    "dialogue_act": "ASK_CONFIRMATION",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["modify_requires_confirmation"]
  }
}
```

确认后再执行：

```json
{
  "name": "transaction.update",
  "requires_confirmation": true,
  "priority": 1,
  "arguments": {
    "transactionId": 100,
    "amount": "32.00",
    "type": "EXPENSE",
    "recordDate": "2026-06-18",
    "description": "午饭"
  }
}
```

### 5.6 删除账单

`transaction.delete` 必须二次确认。不能仅凭“删掉刚才那笔”直接删除，应先查询候选记录并展示确认信息。

### 5.7 创建预算

用户：

```text
给这个月设置3000元预算，80%提醒
```

Planner：

```json
{
  "intent": "BUDGET_MANAGE",
  "confidence": 0.9,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "budget.create",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "cycle": "MONTHLY",
        "periodStart": "2026-06-01",
        "periodEnd": "2026-06-30",
        "limitAmount": "3000.00",
        "alertThreshold": "0.80",
        "enabled": true
      }
    }
  ],
  "reply_style": {
    "scene": "BUDGET",
    "dialogue_act": "CONFIRM_CREATED",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["budget"]
  }
}
```

### 5.8 预算状态与提醒

用户：

```text
这个月预算还剩多少？
```

Planner：

```json
{
  "intent": "BUDGET_QUERY",
  "confidence": 0.95,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "budget.status",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "cycle": "MONTHLY",
        "date": "2026-06-18"
      }
    }
  ],
  "reply_style": {
    "scene": "BUDGET",
    "dialogue_act": "SUMMARIZE_RESULT",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["budget_status"]
  }
}
```

### 5.9 看板分析

用户：

```text
总结一下我这个月的收支
```

Planner：

```json
{
  "intent": "FINANCE_ANALYSIS",
  "confidence": 0.95,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "dashboard.summary",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "period": "MONTH",
        "date": "2026-06-18"
      }
    },
    {
      "name": "dashboard.behavior_tags",
      "requires_confirmation": false,
      "priority": 2,
      "arguments": {
        "ledgerId": 1,
        "period": "MONTH",
        "date": "2026-06-18"
      }
    }
  ],
  "reply_style": {
    "scene": "DASHBOARD_ANALYSIS",
    "dialogue_act": "SUMMARIZE_RESULT",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["dashboard"]
  }
}
```

### 5.10 导出账单

用户：

```text
导出6月账单
```

Planner：

```json
{
  "intent": "EXPORT_DATA",
  "confidence": 0.95,
  "need_user_reply": false,
  "need_confirmation": false,
  "actions": [
    {
      "name": "export.transactions",
      "requires_confirmation": false,
      "priority": 1,
      "arguments": {
        "ledgerId": 1,
        "startDate": "2026-06-01",
        "endDate": "2026-06-30"
      }
    }
  ],
  "reply_style": {
    "scene": "EXPORT",
    "dialogue_act": "SHOW_RESULT",
    "tone": "concise",
    "mood": 35,
    "emoji": "peace",
    "tags": ["export"]
  }
}
```

---

## 6. 财务场景识别规则

记账场景不能只靠生活词判断。以下词单独出现不构成记账：

```text
外卖、奶茶、打车、午饭、购物、电影、咖啡、房租、水电
```

进入 `TRANSACTION_RECORD` 至少满足以下条件之一：

1. 用户明确说“记账、记一笔、帮我记、入账、记录一下”。
2. 用户输入包含明确金额，并出现交易动作：`花了、付了、买了、消费、支出、收入、工资、到账、退款、报销、转账`。
3. 用户输入是典型简写账单，并包含金额，例如“午饭28，地铁6”。

示例：

| 用户输入 | 结果 |
|---|---|
| 外卖又送错了，真的无语 | 日常/烦躁，不记账 |
| 外卖花了28，结果还送错了 | 记账 |
| 帮我记一下外卖28 | 记账 |
| 今天午饭挺难吃 | 不记账 |
| 午饭28，咖啡18 | 记账 |

金额不明确时不要入库，应追问或生成待确认草稿：

```text
打车三十多
```

应输出 `need_confirmation=true`，不要直接创建确定金额。

---

## 7. 风险分级

### 7.1 可自动执行

一般可自动执行：

```text
ledger.list
ledger.get
transaction.list
transaction.get
transaction.parse
budget.list
budget.status
dashboard.summary
dashboard.trend
dashboard.behavior_tags
dashboard.emotion_tags
tag.behavior.list
tag.emotion.list
notification.list
export.transactions
chat.respond
chat.ask_clarification
```

### 7.2 用户明确表达后可执行

用户明确要求时可以执行：

```text
ledger.create
ledger.update
transaction.create
transaction.create_from_text
budget.create
budget.update
notification.mark_read
budget.alert.generate
```

### 7.3 必须二次确认

必须确认：

```text
ledger.delete
ledger.member.add
ledger.member.update
ledger.member.remove
transaction.update
transaction.delete
budget.delete
notification.delete
```

批量操作也必须确认，例如：

```text
批量删除账单
批量修改分类
删除某个账本
移除账本成员
修改成员权限
```

---

## 8. Skill 接口设计

建议接口：

```java
public interface Skill {

    String name();

    String description();

    boolean supports(String actionName);

    SkillResult execute(AssistantAction action, SkillExecutionContext context);
}
```

`SkillExecutionContext` 应包含当前登录用户对象或用户 ID、原始输入、会话 ID、默认账本 ID、dryRun 标志：

```java
public class SkillExecutionContext {

    private Long userId;
    private Long defaultLedgerId;
    private String sessionId;
    private String rawUserMessage;
    private boolean dryRun;
}
```

实际执行时建议直接调用项目现有 Service，而不是从后端内部再次 HTTP 调用 REST：

| Skill | 建议注入 Service |
|---|---|
| LedgerSkill | `LedgerService` |
| LedgerMemberSkill | `LedgerService` |
| TransactionSkill | `TransactionRecordService`、`LlmParsingService` |
| BudgetSkill | `BudgetService`、`LlmParsingService` |
| DashboardSkill | `DashboardService` |
| TagSkill | `TagService` |
| NotificationSkill | `NotificationService` |
| ExportSkill | `ExcelExportService` |

---

## 9. SkillResult

建议结构：

```java
public class SkillResult {

    private boolean success;
    private String actionName;
    private String message;
    private Object data;
    private boolean needUserConfirmation;
    private boolean needUserInput;
    private String errorCode;
}
```

约定：

1. `success=true` 表示后端真实执行成功。
2. `needUserConfirmation=true` 表示 action 未执行，仅生成确认请求。
3. `needUserInput=true` 表示缺少关键参数。
4. `data` 应返回 DTO 或简化后的摘要，供 Renderer 使用。
5. 不应把异常堆栈暴露给用户。

---

## 10. ResponseRenderer 要求

Renderer 根据 `AssistantPlan` 和 `SkillResult` 生成最终回复。

规则：

1. 只有 `SkillResult.success=true` 且写操作真实完成时，才能说“已创建、已修改、已删除、已入账”。
2. 如果 action 未执行，只能说“需要确认”或“还缺少信息”。
3. 查询结果应简短总结，不要把大量原始 DTO 全量塞给用户。
4. 导出结果应交给前端下载处理，不要把二进制返回给 LLM。
5. 最终回复仍需包含项目当前约定的状态标签：

```json
{"mood": 35, "emoji": "peace"}
```

---

## 11. Planner Prompt 要求

Planner 提示词应明确：

```text
你是 EveryCent 的后端 action 规划器。
你不生成最终回复，只输出 JSON。
你只能选择白名单 action。
你不能声称功能已执行。
你不能输出 URL、SQL、Java 类名或方法名。
你必须根据用户意图选择最少数量的 action。
缺少 ledgerId 时，先使用默认账本；没有默认账本则调用 ledger.list 或请求用户选择。
写操作必须按风险等级设置 requires_confirmation。
```

可用 action 必须与本文档第 3 节完全一致。

---

## 12. 与现有 AiAssistant 流程集成

当前入口：

```text
POST /api/assistant/chat
```

建议改造流程：

```text
ChatRequestDTO
  ↓
AssistantPlanner.plan
  ↓
PlanSchemaValidator
  ↓
SkillExecutor.execute
  ↓
ResponseRenderer.render
  ↓
ReplyOutputValidator.validate
  ↓
ChatResponseDTO
```

`ChatResponseDTO` 已有字段：

```text
conversationId
messageId
assistantMessage
userEmotionTagCode
userEmotionConfidence
aiEmotionBefore
aiEmotionAfter
accountingCapture
retrievedMemories
```

如果后续希望前端展示工具调用详情，可新增字段，例如：

```text
executedActions
pendingConfirmation
downloadToken / downloadUrl
```

但这属于代码实现阶段，本文档只定义协议方向。

---

## 13. CLI 调试输出建议

调试模式应输出：

```text
--- PLAN DEBUG ---
planner=llm
intent=TRANSACTION_RECORD
actions=[transaction.create_from_text]
needConfirmation=false
--- PLAN DEBUG END ---

--- SKILL DEBUG ---
action=transaction.create_from_text
success=true
data={transactionId=101}
--- SKILL DEBUG END ---

--- RENDER DEBUG ---
final=已记账：午饭 28 元。
{"mood":35,"emoji":"peace"}
--- RENDER DEBUG END ---
```

对需要确认的操作：

```text
--- PLAN DEBUG ---
intent=TRANSACTION_MODIFY
actions=[transaction.list]
needConfirmation=true
--- PLAN DEBUG END ---

--- SKILL DEBUG ---
action=transaction.list
success=true
data={candidates=[...]}
--- SKILL DEBUG END ---
```

---

## 14. 验收用例

### 14.1 外卖抱怨不记账

输入：

```text
外卖又送错了，真的无语
```

期望：

1. 不调用 `transaction.create`。
2. 不调用 `transaction.create_from_text`。
3. 可以走 `chat.respond`。

### 14.2 外卖金额记账

输入：

```text
外卖花了28
```

期望：

1. 调用 `transaction.create_from_text` 或 `transaction.parse` + `transaction.create`。
2. 金额为 `28`。
3. 类型为 `EXPENSE`。
4. 成功后才能回复已入账。

### 14.3 查询月度收支

输入：

```text
看看我这个月收支怎么样
```

期望：

1. 调用 `dashboard.summary`。
2. 可追加调用 `dashboard.behavior_tags`。
3. 回复包含收入、支出、结余和主要支出分类。

### 14.4 删除账单必须确认

输入：

```text
删掉刚才那笔午饭
```

期望：

1. 先调用 `transaction.list` 查候选。
2. 不直接调用 `transaction.delete`。
3. 回复要求用户确认具体删除哪一笔。

### 14.5 设置预算

输入：

```text
这个月预算设成3000，80%提醒
```

期望：

1. 调用 `budget.create` 或在已有预算时调用 `budget.update`。
2. `cycle=MONTHLY`。
3. `limitAmount=3000.00`。
4. `alertThreshold=0.80`。

### 14.6 导出账单

输入：

```text
导出6月账单
```

期望：

1. 调用 `export.transactions`。
2. 返回下载结果给前端。
3. 不把 Excel 内容交给 LLM 总结。

---

## 15. 注意事项

1. Skill 文档必须跟随真实 Controller、Service、DTO 更新。
2. 不要再使用 `FinanceRecordService`、`finance.create_records` 等当前项目不存在的名称。
3. 不要保留角色扮演、人设、皓尾、本龙等内容。
4. Planner 不处理鉴权，鉴权必须由后端现有 Service 执行。
5. Planner 输出的 ID 不能被直接信任，Service 必须校验当前用户是否有权限访问对应资源。
6. 写操作失败时，Renderer 不能假装成功。
7. 高风险操作必须二次确认。
8. 默认账本选择应由后端会话、用户偏好或前端上下文提供；Planner 不应凭空猜测账本 ID。
9. 所有日期应使用 ISO-8601，例如 `2026-06-18`。
10. 金额应使用字符串形式传递给后端 DTO，避免精度问题。
