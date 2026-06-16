# LLM DTO 单元测试报告

## 1. 测试基本信息

- 项目名称：EveryCent
- 测试对象：LLM 后端详细设计文档第 6 节 DTO
- 测试日期：2026-06-16
- 测试方式：JUnit 5 单元测试
- 测试类：`com.everycent.llm.dto.LlmDtoUnitTest`
- 测试文件：`src/test/java/com/everycent/llm/dto/LlmDtoUnitTest.java`
- 是否依赖前端：否
- 是否依赖数据库：否
- 是否启动 Spring 上下文：否

## 2. 测试命令

```bash
./mvnw -Dskip.installnodenpm -Dskip.npm -Dtest=LlmDtoUnitTest test -ntp --batch-mode
```

## 3. 系统实际输出摘要

```text
[INFO] Running com.everycent.llm.dto.LlmDtoUnitTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.443 s -- in com.everycent.llm.dto.LlmDtoUnitTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

说明：Maven Enforcer 在测试过程中输出了既有依赖收敛警告，涉及 `javax.xml.bind:jaxb-api`、`com.typesafe:config`、`org.apache.commons:commons-text`，但本次 DTO 单元测试执行成功，未因此失败。

## 4. 测试结论总览

| 序号 | 测试点 | 预计输出 | 系统实际输出 | 是否通过 |
| --- | --- | --- | --- | --- |
| 1 | `TransactionParseRequestDTO` 必填字段校验 | `ledgerId`、`text` 产生校验违规 | 校验违规字段包含 `ledgerId`、`text` | 通过 |
| 2 | `TransactionParseRequestDTO.text` 最大长度校验 | 501 字符文本产生 `text` 校验违规 | 校验违规字段为 `text` | 通过 |
| 3 | `TransactionParseRequestDTO` JSON 序列化与反序列化 | JSON 包含 `ledgerId=1`、`text=午饭 25 元`、`transactionDate=2026-06-16`，反序列化后字段一致 | JSON 包含指定字段，反序列化后 `ledgerId=1`、`text=午饭 25 元`、`transactionDate=2026-06-16` | 通过 |
| 4 | `TransactionParseResultDTO` 字段读写 | 文档定义字段均可正确 set/get | 金额、类型、行为标签、情绪标签、日期、描述、置信度、确认标记、原始输入均与设置值一致 | 通过 |
| 5 | `TransactionParseResultDTO` JSON 序列化 | JSON 包含 `amount=25.50`、`type=EXPENSE`、`transactionDate=2026-06-16`、`needUserConfirm=false` | JSON 包含上述字段和值 | 通过 |
| 6 | `AiAlertRequestDTO` 必填字段校验 | `ledgerId`、`budgetId` 产生校验违规 | 校验违规字段包含 `ledgerId`、`budgetId` | 通过 |
| 7 | `AiAlertRequestDTO` 字段读写与 JSON 序列化 | JSON 包含 `ledgerId=1`、`budgetId=2`、`saveAsNotification=true`，反序列化后字段一致 | JSON 包含指定字段，反序列化后 `ledgerId=1`、`budgetId=2`、`saveAsNotification=true` | 通过 |
| 8 | `AiAlertResultDTO` 字段读写与 JSON 序列化 | JSON 包含标题、内容、`level=WARNING`、预算金额、比例、通知标记和通知 ID | 字段读写一致，JSON 包含 `title=预算预警`、`content=本月餐饮预算已使用 85%`、`level=WARNING`、`usedAmount=850.00`、`limitAmount=1000.00`、`usedRatio=0.85`、`notificationId=99` | 通过 |

## 5. 各 DTO 测试结果

### 5.1 TransactionParseRequestDTO

| 测试内容 | 预计输出 | 系统实际输出 | 是否通过 |
| --- | --- | --- | --- |
| `ledgerId` 为空、`text` 为空字符串 | Bean Validation 返回 `ledgerId` 与 `text` 两个违规字段 | 实际返回违规字段包含 `ledgerId`、`text` | 通过 |
| `text` 长度为 501 | Bean Validation 返回 `text` 违规字段 | 实际返回违规字段为 `text` | 通过 |
| JSON 序列化 | 日期按 ISO 字符串输出，字段名保持文档格式 | JSON 包含 `"ledgerId":1`、`"text":"午饭 25 元"`、`"transactionDate":"2026-06-16"` | 通过 |
| JSON 反序列化 | 反序列化对象字段与原 DTO 一致 | `ledgerId=1`、`text=午饭 25 元`、`transactionDate=2026-06-16` | 通过 |

### 5.2 TransactionParseResultDTO

| 测试内容 | 预计输出 | 系统实际输出 | 是否通过 |
| --- | --- | --- | --- |
| 金额字段 | `amount=25.50` | `amount=25.50` | 通过 |
| 交易类型字段 | `type=EXPENSE` | `type=EXPENSE` | 通过 |
| 行为标签字段 | `behaviorTagCode=FOOD`、`behaviorTagName=餐饮`、`behaviorTagId=11` | 实际值与预计一致 | 通过 |
| 情绪标签字段 | `emotionTagCode=HAPPY`、`emotionTagName=开心`、`emotionTagId=21` | 实际值与预计一致 | 通过 |
| 日期、描述、置信度、确认标记、原始输入 | `transactionDate=2026-06-16`、`description=午饭`、`confidence=0.92`、`needUserConfirm=false`、`rawInput=午饭 25.5` | 实际值与预计一致 | 通过 |
| JSON 序列化 | JSON 包含金额、枚举、日期、布尔字段 | JSON 包含 `"amount":25.50`、`"type":"EXPENSE"`、`"transactionDate":"2026-06-16"`、`"needUserConfirm":false` | 通过 |

### 5.3 AiAlertRequestDTO

| 测试内容 | 预计输出 | 系统实际输出 | 是否通过 |
| --- | --- | --- | --- |
| `ledgerId` 与 `budgetId` 为空 | Bean Validation 返回 `ledgerId` 与 `budgetId` 两个违规字段 | 实际返回违规字段包含 `ledgerId`、`budgetId` | 通过 |
| 字段读写 | `ledgerId=1`、`budgetId=2`、`saveAsNotification=true` | 实际值与预计一致 | 通过 |
| JSON 序列化与反序列化 | JSON 字段名和值符合文档，反序列化后字段一致 | JSON 包含 `"ledgerId":1`、`"budgetId":2`、`"saveAsNotification":true`，反序列化后值一致 | 通过 |

### 5.4 AiAlertResultDTO

| 测试内容 | 预计输出 | 系统实际输出 | 是否通过 |
| --- | --- | --- | --- |
| 基础文本字段 | `title=预算预警`、`content=本月餐饮预算已使用 85%` | 实际值与预计一致 | 通过 |
| 告警级别枚举 | `level=WARNING` | `level=WARNING` | 通过 |
| 预算状态字段 | `overBudget=false`、`needNotification=true`、`notificationId=99` | 实际值与预计一致 | 通过 |
| 金额与比例字段 | `usedAmount=850.00`、`limitAmount=1000.00`、`usedRatio=0.85` | 实际值与预计一致 | 通过 |
| JSON 序列化 | JSON 包含文档定义字段和值 | JSON 包含标题、内容、告警级别、超预算标记、金额、比例、通知标记、通知 ID | 通过 |

## 6. 最终结论

本次针对四个 LLM DTO 共执行 8 个单元测试，结果为：

- 测试总数：8
- 失败数：0
- 错误数：0
- 跳过数：0
- 构建结果：`BUILD SUCCESS`

结论：四个 DTO 在当前实现下满足本次单元测试覆盖的数据承载、Bean Validation 校验、JSON 序列化与反序列化要求。
