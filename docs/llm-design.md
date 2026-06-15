# LLM 模块设计

EveryCent 的 LLM 模块用于辅助记账，而不是替代后端业务校验。AI 输出只能作为候选结果，最终入库前必须经过后端规则检查。

## 模块职责

- 自然语言消费记录解析：从用户输入中识别金额、日期、备注、收入或支出类型。
- 行为标签识别：将文本归类为餐饮、娱乐、交通、工资收入等受控标签。
- 情绪标签识别：识别开心、焦虑、冲动、后悔等情绪标签。
- 个性化预算告警文案生成：结合预算使用情况生成提醒内容。

## 建议包结构

- `com.everycent.llm`：LLM 服务接口和协调逻辑。
- `com.everycent.llm.dto`：LLM 请求、响应和解析结果 DTO。
- `com.everycent.llm.prompt`：提示词模板、标签枚举说明和输出格式约束。
- `com.everycent.llm.client`：LLM API HTTP 客户端和超时配置。

当前已预留：

- `LlmClient`
- `LlmPromptService`
- `LlmParsingService`
- `dto/TransactionParseResult`

## 自然语言记账流程

1. 用户在前端 AI 录入页面输入自然语言。
2. 后端接收文本并检查长度、空值和账本权限。
3. `LlmPromptService` 组装提示词，明确要求返回结构化 JSON。
4. `LlmClient` 调用 LLM API。
5. `LlmParsingService` 解析返回结果。
6. 后端校验金额、收支类型、行为标签、情绪标签和账本权限。
7. 校验通过后返回给前端确认，或由用户确认后保存。

## AI 风险控制

- 金额必须由后端校验，不能直接信任 LLM 输出。
- 标签必须限定在系统枚举范围内，未知标签应映射为 `OTHER` 或要求用户手动选择。
- LLM 调用超时、失败或返回格式错误时，应降级为手动表单录入。
- 不允许 AI 结果直接无校验入库。
- 预算提醒文案不能包含敏感、歧视、恐吓或误导性内容。
- LLM API Key 只能通过环境变量或安全配置注入，不能写入代码仓库。

## 输出格式建议

自然语言解析建议要求 LLM 返回如下结构：

```json
{
  "amount": 36.5,
  "type": "EXPENSE",
  "behaviorTag": "FOOD",
  "moodTag": "HAPPY",
  "remark": "午餐",
  "confidence": 0.86,
  "needsManualReview": false
}
```

后端应对每个字段进行类型、范围和枚举校验。
