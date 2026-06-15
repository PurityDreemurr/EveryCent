# 接口设计规划

本文档为 EveryCent 课程设计接口规划，先描述目标接口，不要求当前全部实现。实际开发时应结合 Spring Security、JWT 和 JHipster 现有用户体系。

## 用户认证接口

### 登录

- 方法：`POST`
- 路径：`/api/authenticate`
- 功能：用户登录并获取 JWT。
- 请求示例：

```json
{
  "username": "admin",
  "password": "admin",
  "rememberMe": true
}
```

- 返回示例：

```json
{
  "id_token": "jwt-token"
}
```

### 获取当前用户

- 方法：`GET`
- 路径：`/api/account`
- 功能：获取当前登录用户信息。
- 请求示例：无请求体。
- 返回示例：

```json
{
  "login": "user",
  "email": "user@example.com",
  "authorities": ["ROLE_USER"]
}
```

## 账本接口

### 创建账本

- 方法：`POST`
- 路径：`/api/ledgers`
- 功能：创建个人或共享账本。
- 请求示例：

```json
{
  "name": "我的日常账本",
  "description": "记录日常消费和收入"
}
```

- 返回示例：

```json
{
  "id": 1,
  "name": "我的日常账本",
  "ownerLogin": "user"
}
```

### 查询账本列表

- 方法：`GET`
- 路径：`/api/ledgers`
- 功能：查询当前用户可访问的账本。
- 请求示例：无请求体。
- 返回示例：

```json
[
  {
    "id": 1,
    "name": "我的日常账本",
    "permission": "OWNER"
  }
]
```

## 收支记录接口

### 新增收支记录

- 方法：`POST`
- 路径：`/api/transactions`
- 功能：通过传统表单新增收入或支出。
- 请求示例：

```json
{
  "ledgerId": 1,
  "amount": 36.5,
  "type": "EXPENSE",
  "behaviorTag": "FOOD",
  "moodTag": "HAPPY",
  "occurredDate": "2026-06-15",
  "remark": "午餐"
}
```

- 返回示例：

```json
{
  "id": 100,
  "ledgerId": 1,
  "amount": 36.5,
  "type": "EXPENSE",
  "monthBalance": 2963.5
}
```

### 查询收支记录

- 方法：`GET`
- 路径：`/api/transactions?ledgerId=1&month=2026-06`
- 功能：按账本和月份查询记录。
- 请求示例：查询参数见路径。
- 返回示例：

```json
[
  {
    "id": 100,
    "amount": 36.5,
    "type": "EXPENSE",
    "behaviorTag": "FOOD",
    "moodTag": "HAPPY",
    "remark": "午餐"
  }
]
```

## 自然语言记账接口

### 解析自然语言

- 方法：`POST`
- 路径：`/api/ai/transactions/parse`
- 功能：调用 LLM 从自然语言中识别金额、收支类型、行为标签和情绪标签。
- 请求示例：

```json
{
  "ledgerId": 1,
  "text": "今天午餐花了36.5元，心情还不错"
}
```

- 返回示例：

```json
{
  "amount": 36.5,
  "type": "EXPENSE",
  "behaviorTag": "FOOD",
  "moodTag": "HAPPY",
  "confidence": 0.86,
  "needsManualReview": false
}
```

## 预算接口

### 设置预算

- 方法：`POST`
- 路径：`/api/budgets`
- 功能：设置周预算或月预算。
- 请求示例：

```json
{
  "ledgerId": 1,
  "periodType": "MONTH",
  "period": "2026-06",
  "amount": 3000
}
```

- 返回示例：

```json
{
  "id": 20,
  "ledgerId": 1,
  "periodType": "MONTH",
  "amount": 3000,
  "usedAmount": 1200
}
```

## 数据看板接口

### 获取看板汇总

- 方法：`GET`
- 路径：`/api/dashboard/summary?ledgerId=1&month=2026-06`
- 功能：返回收入、支出、余额和分类占比。
- 请求示例：查询参数见路径。
- 返回示例：

```json
{
  "income": 5000,
  "expense": 2036.5,
  "balance": 2963.5,
  "categoryRatio": [
    {
      "behaviorTag": "FOOD",
      "amount": 860.5,
      "ratio": 0.42
    }
  ]
}
```

## Excel 导出接口

### 导出账单

- 方法：`GET`
- 路径：`/api/transactions/export?ledgerId=1&month=2026-06`
- 功能：导出指定账本月份账单 Excel。
- 请求示例：查询参数见路径。
- 返回示例：返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 文件流。

## AI 预警接口

### 生成预算提醒

- 方法：`POST`
- 路径：`/api/ai/budget-warning`
- 功能：结合预算使用情况和 LLM 生成个性化提醒文案。
- 请求示例：

```json
{
  "ledgerId": 1,
  "period": "2026-06",
  "usedAmount": 2900,
  "budgetAmount": 3000
}
```

- 返回示例：

```json
{
  "overBudget": false,
  "riskLevel": "HIGH",
  "message": "本月预算已接近上限，建议减少非必要支出。"
}
```
