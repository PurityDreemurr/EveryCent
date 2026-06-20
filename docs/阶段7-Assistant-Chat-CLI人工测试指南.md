# 阶段 7 Assistant Chat CLI 人工测试指南

## 目标

验证 `/api/assistant/chat` 已接入 Planner、SkillRouter、SkillResult 汇总和 ResponseRenderer，并能在 CLI/模型对话中覆盖自然语言查账、记账、预算、禁止操作和下载结果。

## 启动方式

在开发容器内运行：

```bash
docker exec -it everycent-dev-dev-1 /workspace/scripts/assistant-dialogue-cli.sh
```

如果要直接观察后端接口日志，另开一个终端：

```bash
docker exec -it everycent-dev-dev-1 tail -f /workspace/target/spring.log
```

日志应能看到类似信息：

```text
Assistant plan generated intent=TRANSACTION_RECORD, actions=[transaction.create_from_text]
Assistant skill results=[transaction.create_from_text:true:null]
```

日志只应包含 intent、action 名称、成功状态和错误码，不应打印用户完整原文、账号密码、token、导出内容或完整账单明细。

## 前置条件

1. 当前用户已登录。
2. 至少存在一个可读写账本。
3. CLI 或前端请求中带有 `ledgerId`；如果没有账本上下文，应返回选择账本提示。
4. LLM 配置可用时，自然语言记账会调用解析并入库；LLM 不可用时，应返回执行失败卡片，不应假装成功。

## 测试用例

### 1. 自然语言记账

输入：

```text
午饭28，咖啡18
```

期望：

1. Planner action 为 `transaction.create_from_text`。
2. SkillRouter 执行成功后返回 `transaction_created` 卡片。
3. `accountingCapture.created=true`。
4. 回复不应只说“好，那就先这样”，而应体现已记账或失败原因。

### 2. 自然语言查账

输入：

```text
查一下本月账单
```

期望：

1. Planner action 为 `transaction.list`。
2. 返回 `query_result` 卡片。
3. SkillResult 中 `success=true`。

### 3. 自然语言设置预算

输入：

```text
这个月预算设成3000，80%提醒
```

期望：

1. Planner action 为 `budget.create`。
2. 参数包含 `cycle=MONTHLY`、`limitAmount=3000`、`alertThreshold=0.80`。
3. 返回 `budget_saved` 卡片。

### 4. 预算状态查询

输入：

```text
这个月预算还剩多少？
```

期望：

1. Planner action 为 `budget.status`。
2. 返回 `query_result` 卡片。
3. 回复说明已查询预算状态。

### 5. 删除请求被拒绝

输入：

```text
删掉刚才那笔午饭
```

期望：

1. Planner 可以产生 `transaction.delete`，但必须由 SkillRouter/Policy 拦截。
2. 返回 `policy_blocked` 卡片。
3. 回复应温和说明 AI 不能执行删除，并引导到交易详情页或账单列表页手动删除。
4. 不得调用任何真实删除服务。

### 6. 账户请求被拒绝或引导

输入：

```text
帮我改一下账号密码
```

期望：

1. Planner action 为 `account.update` 或同类账户 action。
2. SkillRouter/Policy 拦截。
3. 返回 `policy_blocked` 卡片。
4. 回复引导用户到账号设置页面手动处理。

### 7. 导出账单

输入：

```text
导出本月账单
```

期望：

1. Planner action 为 `export.transactions`。
2. 返回 `download_result` 卡片。
3. 返回下载元数据，不把 Excel 二进制内容交给 LLM 或直接打印到对话里。

## 前端联调检查

打开 AI 记账页面后，依次输入上述用例，检查 UI：

1. 普通闲聊显示普通回复。
2. 查询请求显示查询结果卡片。
3. 记账成功显示记账成功卡片。
4. 缺少账本或需要确认时显示待确认卡片。
5. 删除/账户请求显示禁止执行提示。
6. 导出请求显示下载结果卡片。

## 回归要求

每次修改阶段 7 后至少运行：

```bash
docker exec everycent-dev-dev-1 ./mvnw -Dskip.installnodenpm=true -Dskip.npm=true -Dtest='com.everycent.assistant.*Test,com.everycent.assistant.skill.*Test,com.everycent.assistant.accounting.*Test,com.everycent.assistant.validation.*Test' test
```
