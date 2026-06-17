# EveryCent AI 助理、MeCOT 情绪状态与 RAG 长期记忆详细设计文档

> 适用项目：EveryCent 智能记账系统  
> 设计目标：在尽量保留 EveryCent 原有账本、标签、交易、预算和 LLM 解析设计的基础上，新增一个“以 AI 对话为主入口、自动捕获记账内容、具备 MeCOT 情绪状态转移与 RAG 长期记忆”的 AI 私人助理能力。  
> 设计原则：不重写原有业务模型，不混用用户标签与 AI 情绪状态；新增能力通过独立包、独立表、独立接口接入现有系统。

---

## 1. 背景与目标

EveryCent 原有定位是“具有 AI 辅助功能的智能记账系统”，AI 主要用于自然语言记账解析、行为标签识别、用户情绪标签识别和预算提醒文案生成。

本次设计希望将产品入口调整为“私人 AI 助理对话界面”：

1. 用户打开系统后，主界面是 AI 对话。
2. 用户可以与 AI 聊任何内容，不局限于财务。
3. 当用户输入包含明确收支内容时，AI 自动识别、抽取并写入后端账本。
4. 当信息不完整或置信度不足时，AI 通过自然对话追问确认。
5. AI 使用 MeCOT 维护自身长期稳定的情绪状态与回复风格。
6. 系统使用云端 embedding 模型和本地向量数据库保存长期记忆。
7. 每次用户输入都会通过 RAG 检索 top-k 相关记忆，并传入 LLM 作为上下文。
8. 记忆保存时记录用户当时的情绪标签，用于后续判断对话风格。

---

## 2. 总体设计边界

### 2.1 保留原有设计

以下 EveryCent 原有模块继续保留：

| 原有模块 | 保留方式 |
|---|---|
| `User` / JWT / Spring Security | 继续作为用户身份与权限基础 |
| `Ledger` | 继续作为账本实体 |
| `TransactionRecord` | 继续作为收支记录实体 |
| `BehaviorTag` | 继续表示用户消费行为标签 |
| `EmotionTag` | 继续表示用户消费或表达时的用户情绪标签 |
| `Budget` / `NotificationMessage` | 继续负责预算与提醒 |
| `LlmClient` | 继续作为云端 LLM 调用基础 |
| `TransactionPromptBuilder` | 可复用并扩展为对话中的交易抽取 prompt |
| `AiResultGuardService` | 继续负责 LLM 输出入库前校验 |

### 2.2 新增设计

新增模块不替代原有记账模块，而是在对话入口上方增加一层 AI 助理编排：

```text
用户输入
  -> AI 对话入口
  -> 用户情绪识别
  -> 当前输入 embedding
  -> 本地向量库 RAG 检索 top-k 长期记忆
  -> 读取 AI 自身 MeCOT 情绪状态
  -> 组装 LLM 对话上下文
  -> LLM 生成回复
  -> 记账意图检测与交易抽取
      -> 无记账内容：只保存对话与记忆
      -> 高置信度：自动入库交易记录
      -> 低置信度：生成追问，等待用户确认
  -> 保存用户消息、AI 回复、用户情绪标签、AI 情绪转移、向量记忆
```

---

## 3. 标签与情绪体系设计

### 3.1 必须分离的三类概念

| 概念 | 归属 | 数据位置 | 用途 |
|---|---|---|---|
| 用户行为标签 | 用户/交易 | `behavior_tag`、`transaction_record.behavior_tag_id` | 餐饮、交通、购物、娱乐等消费行为分类 |
| 用户情绪标签 | 用户/交易/记忆 | `emotion_tag`、`transaction_record.emotion_tag_id`、`ai_memory.user_emotion_tag_id` | 用户消费或表达时的情绪状态 |
| AI 情绪状态 | AI 助理 | 新增 `ai_emotion_state` | MeCOT 维护 AI 自身当前情绪和回复风格 |

### 3.2 用户情绪标签

EveryCent 原有 `emotion_tag` 继续表示用户情绪，例如：

```text
HAPPY
CALM
IMPULSIVE
ANXIOUS
REGRET
STRESSED
NONE
```

它可以绑定到：

1. 一条交易记录。
2. 一条用户消息。
3. 一条长期记忆。

### 3.3 AI 情绪状态

AI 情绪状态不使用 `emotion_tag` 表。建议使用 MeCOT 12 状态模型：

```text
surprised
happy
pleased
fearful
angry
grieved
sad
disgusted
depressed
tired
calm
relieved
```

AI 情绪状态用于控制 AI 的对话风格，例如：

| AI 状态 | 建议回复风格 |
|---|---|
| `calm` | 平稳、简洁、理性 |
| `pleased` | 温和、认可、轻量鼓励 |
| `relieved` | 放松、缓和、降低压力 |
| `sad` | 共情、低刺激、不催促 |
| `fearful` | 谨慎、风险提示更明显 |
| `angry` | 不建议直接外显愤怒，应映射为严肃、边界清晰 |
| `tired` | 简短、减少冗余 |

---

## 4. 新增包结构

建议在原有 `com.everycent.llm` 模块基础上扩展，不迁移原有类。

```text
src/main/java/com/everycent/
├── web/rest/
│   └── AiAssistantResource.java
├── domain/
│   ├── AiConversation.java
│   ├── AiMessage.java
│   ├── AiMemory.java
│   └── AiEmotionState.java
├── repository/
│   ├── AiConversationRepository.java
│   ├── AiMessageRepository.java
│   ├── AiMemoryRepository.java
│   └── AiEmotionStateRepository.java
├── assistant/
│   ├── AiAssistantService.java
│   ├── AiAssistantOrchestrator.java
│   ├── dto/
│   │   ├── ChatRequestDTO.java
│   │   ├── ChatResponseDTO.java
│   │   ├── MemoryContextDTO.java
│   │   ├── AccountingCaptureDTO.java
│   │   └── EmotionTransitionDTO.java
│   ├── emotion/
│   │   ├── MecotEmotionService.java
│   │   ├── AiEmotionStateService.java
│   │   ├── AiEmotionStateModel.java
│   │   ├── AiEmotionTransitionResult.java
│   │   └── AiEmotionPromptAdapter.java
│   ├── memory/
│   │   ├── AiMemoryService.java
│   │   ├── EmbeddingClient.java
│   │   ├── VectorStoreClient.java
│   │   ├── MemoryRetrievalService.java
│   │   ├── MemoryImportanceService.java
│   │   └── MemorySummarizationService.java
│   ├── accounting/
│   │   ├── AccountingIntentService.java
│   │   ├── ConversationTransactionExtractor.java
│   │   └── PendingAccountingConfirmationService.java
│   └── prompt/
│       ├── AssistantPromptBuilder.java
│       ├── UserEmotionPromptBuilder.java
│       ├── AccountingIntentPromptBuilder.java
│       └── MemorySummarizationPromptBuilder.java
└── config/
    ├── AiAssistantProperties.java
    └── VectorStoreProperties.java
```

说明：

1. `assistant` 是新增 AI 助理主模块。
2. `assistant.emotion` 只处理 AI 自身 MeCOT 状态。
3. `assistant.memory` 只处理长期记忆、embedding 和向量库。
4. `assistant.accounting` 只处理对话中的记账意图与交易抽取。
5. `llm` 原有包继续保留，作为通用 LLM 调用、解析、prompt 基础设施。

---

## 5. 新增数据库表设计

### 5.1 `ai_conversation`

保存用户与 AI 的对话会话。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `user_id` | bigint | 当前用户 |
| `title` | varchar(200) | 会话标题，可由 LLM 总结 |
| `created_date` | datetime | 创建时间 |
| `last_message_date` | datetime | 最近消息时间 |
| `archived` | boolean | 是否归档 |

索引：

```text
idx_ai_conversation_user_last_message(user_id, last_message_date)
```

### 5.2 `ai_message`

保存每轮消息。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `conversation_id` | bigint | 所属会话 |
| `user_id` | bigint | 用户 |
| `role` | varchar(30) | `USER` / `ASSISTANT` / `SYSTEM` |
| `content` | text | 消息正文 |
| `user_emotion_tag_id` | bigint nullable | 用户消息的用户情绪标签，仅 `USER` 消息通常有值 |
| `user_emotion_confidence` | decimal(5,4) nullable | 用户情绪识别置信度 |
| `ai_emotion_before` | varchar(50) nullable | AI 回复前 MeCOT 状态 |
| `ai_emotion_after` | varchar(50) nullable | AI 回复后 MeCOT 状态 |
| `extracted_transaction_id` | bigint nullable | 本轮自动创建的交易记录 |
| `created_date` | datetime | 创建时间 |

索引：

```text
idx_ai_message_conversation_created(conversation_id, created_date)
idx_ai_message_user_created(user_id, created_date)
```

### 5.3 `ai_memory`

保存长期记忆元数据。向量本体存储在本地向量数据库中。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 |
| `conversation_id` | bigint nullable | 来源会话 |
| `message_id` | bigint nullable | 来源消息 |
| `memory_type` | varchar(50) | `MESSAGE` / `SUMMARY` / `USER_PREFERENCE` / `FINANCIAL_PATTERN` / `FACT` |
| `content` | text | 可直接传给 LLM 的记忆内容 |
| `source_text` | text nullable | 原始文本 |
| `user_emotion_tag_id` | bigint nullable | 用户说这句话时的情绪标签 |
| `user_emotion_confidence` | decimal(5,4) nullable | 情绪置信度 |
| `behavior_tag_id` | bigint nullable | 可选行为标签 |
| `vector_id` | varchar(100) | 向量库中的 point id |
| `importance_score` | decimal(5,4) | 重要性分数 |
| `created_date` | datetime | 创建时间 |
| `last_accessed_date` | datetime nullable | 最近被召回时间 |
| `access_count` | int | 被召回次数 |

索引：

```text
idx_ai_memory_user_type(user_id, memory_type)
idx_ai_memory_user_emotion(user_id, user_emotion_tag_id)
idx_ai_memory_importance(user_id, importance_score)
```

### 5.4 `ai_emotion_state`

保存 AI 助理针对每个用户的 MeCOT 内部情绪状态。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户 |
| `current_emotion` | varchar(50) | 当前 AI 情绪状态 |
| `valence` | decimal(8,5) | 当前效价 |
| `arousal` | decimal(8,5) | 当前唤醒度 |
| `personality_weights_json` | text | 12 维人格权重 |
| `transition_matrix_json` | longtext nullable | 当前转移矩阵，可选持久化 |
| `last_user_emotion_tag_id` | bigint nullable | 最近识别到的用户情绪 |
| `updated_date` | datetime | 更新时间 |

唯一约束：

```text
uk_ai_emotion_state_user(user_id)
```

---

## 6. 本地向量数据库设计

推荐使用 Qdrant 本地 Docker 服务。MySQL 保存元数据，Qdrant 保存向量索引。

### 6.1 Collection

建议 collection 名称：

```text
everycent_ai_memory
```

### 6.2 Point Payload

向量库 payload 建议保存如下字段，用于过滤和重排：

```json
{
  "memoryId": 123,
  "userId": 1,
  "conversationId": 9,
  "messageId": 88,
  "memoryType": "MESSAGE",
  "userEmotionTagCode": "STRESSED",
  "importanceScore": 0.82,
  "createdDate": "2026-06-17T21:30:00Z"
}
```

### 6.3 向量检索范围

每次 RAG 检索必须按 `userId` 过滤，禁止跨用户召回记忆。

```text
filter:
  userId == currentUser.id
```

### 6.4 top-k 默认值

建议配置：

```yaml
app:
  assistant:
    rag:
      top-k: 5
      candidate-limit: 20
      min-score: 0.25
```

流程：

1. 先从向量库取 `candidate-limit` 条候选。
2. 后端按综合分数重排。
3. 最终传给 LLM 的记忆控制在 `top-k` 条。

---

## 7. RAG 长期记忆流程

### 7.1 每次用户输入的检索流程

```text
ChatRequestDTO
  -> sanitize 用户输入
  -> 识别用户情绪标签
  -> EmbeddingClient.embed(userInput)
  -> VectorStoreClient.search(userId, queryVector, candidateLimit)
  -> MemoryRetrievalService rerank
  -> 返回 top-k MemoryContextDTO
```

### 7.2 重排分数

建议综合：

```text
final_score =
  0.65 * vector_similarity
+ 0.15 * importance_score
+ 0.10 * recency_score
+ 0.10 * emotion_relevance_score
```

说明：

1. `vector_similarity` 来自向量库。
2. `importance_score` 来自 `ai_memory.importance_score`。
3. `recency_score` 根据创建时间和最近访问时间计算。
4. `emotion_relevance_score` 根据当前用户情绪与历史记忆情绪是否相同或相近计算。

### 7.3 记忆写入流程

每轮对话结束后，保存：

```text
用户消息
AI 回复
用户情绪标签
AI 情绪转移
自动记账结果
重要性评分
embedding 向量
ai_memory 元数据
```

不是每条消息都必须长期保存为高价值记忆。可以设置规则：

| 场景 | 是否写入长期记忆 |
|---|---|
| 用户表达长期偏好 | 是 |
| 用户表达稳定财务习惯 | 是 |
| 用户明确要求 AI 记住 | 是 |
| 用户只说“你好” | 可只保存消息，不写长期记忆 |
| 自动记账成功的消费事件 | 是，可作为 `FINANCIAL_PATTERN` |
| 敏感且无长期价值内容 | 谨慎写入或不写入 |

---

## 8. MeCOT AI 情绪状态设计

### 8.1 状态空间

AI 状态采用 12 维情绪圆环模型：

| 状态 | valence | arousal |
|---|---:|---:|
| `surprised` | 0.383 | 0.924 |
| `happy` | 0.707 | 0.707 |
| `pleased` | 0.924 | 0.383 |
| `fearful` | -0.383 | 0.924 |
| `angry` | -0.707 | 0.707 |
| `grieved` | -0.924 | 0.383 |
| `sad` | -0.924 | -0.383 |
| `disgusted` | -0.707 | -0.707 |
| `depressed` | -0.383 | -0.924 |
| `tired` | 0.383 | -0.924 |
| `calm` | 0.707 | -0.707 |
| `relieved` | 0.924 | -0.383 |

### 8.2 状态转移输入

MeCOT 不直接使用用户交易标签。它接收以下输入：

```text
当前 AI 情绪状态
当前用户情绪标签
当前用户输入语义
RAG 召回的长期记忆
最近若干轮对话摘要
```

### 8.3 用户情绪到 AI 状态变化的建议映射

| 用户情绪 | 对 AI 状态的影响 |
|---|---|
| `HAPPY` | AI 更可能转向 `pleased` / `happy` |
| `CALM` | AI 更可能保持 `calm` / `relieved` |
| `ANXIOUS` | AI 更可能转向 `calm` / `fearful`，体现谨慎和安抚 |
| `REGRET` | AI 更可能转向 `sad` / `calm`，体现共情和稳定 |
| `STRESSED` | AI 更可能转向 `calm` / `grieved`，降低刺激 |
| `IMPULSIVE` | AI 更可能转向 `fearful` / `calm`，提高风险提醒 |
| `NONE` | AI 主要依据语义和上一状态平滑转移 |

### 8.4 回复风格约束

AI 情绪状态不能导致攻击、羞辱、责备或强烈操控。对于高风险状态应做风格映射：

| AI 内部状态 | 对外表达 |
|---|---|
| `angry` | 严肃、边界清楚，不外显愤怒 |
| `fearful` | 谨慎提醒，不制造恐慌 |
| `depressed` | 低刺激、简短、支持性表达 |
| `disgusted` | 不表达厌恶，转换为“需要谨慎处理” |

---

## 9. 对话与自动记账流程

### 9.1 完整时序

```mermaid
sequenceDiagram
    participant U as User
    participant R as AiAssistantResource
    participant O as AiAssistantOrchestrator
    participant E as UserEmotionService
    participant M as MemoryRetrievalService
    participant S as MecotEmotionService
    participant L as LlmClient
    participant A as AccountingIntentService
    participant T as TransactionRecordService
    participant DB as MySQL/Qdrant

    U->>R: POST /api/assistant/chat
    R->>O: chat(request, currentUser)
    O->>E: detectUserEmotion(message)
    E-->>O: EmotionTag + confidence
    O->>M: retrieveTopK(user, message, emotion)
    M->>DB: embedding search + metadata query
    DB-->>M: candidate memories
    M-->>O: top-k memory contexts
    O->>S: transition(user, message, emotion, memories)
    S->>DB: load/save ai_emotion_state
    S-->>O: before/after AI emotion
    O->>A: detectAndExtract(message, context)
    A-->>O: no intent / candidate transaction / need confirmation
    O->>L: complete(assistant prompt)
    L-->>O: assistant reply
    alt high confidence accounting
        O->>T: create transaction after guard validation
        T->>DB: insert transaction_record
    end
    O->>DB: save ai_message, ai_memory, vectors
    O-->>R: ChatResponseDTO
    R-->>U: assistant reply + optional accounting result
```

### 9.2 自动记账规则

| 识别结果 | 系统行为 |
|---|---|
| 无记账意图 | 只正常聊天，保存对话和必要记忆 |
| 有金额、日期、类型、账本清晰 | 自动入库，并在回复中轻提示 |
| 金额清晰但类别不确定 | 使用默认 `OTHER` 或自然追问 |
| 收入/支出不确定 | 追问确认，不入库 |
| 金额缺失 | 不入库，追问 |
| 置信度低于阈值 | 不入库，追问 |

建议默认阈值：

```yaml
app:
  assistant:
    accounting:
      auto-create-confidence-threshold: 0.85
      ask-confirm-confidence-threshold: 0.60
```

---

## 10. REST API 设计

### 10.1 发送对话消息

```http
POST /api/assistant/chat
```

请求：

```json
{
  "conversationId": 1,
  "message": "我今天又加班到很晚，回家路上买了杯 28 块的奶茶，感觉有点罪恶。",
  "ledgerId": 10
}
```

响应：

```json
{
  "conversationId": 1,
  "messageId": 101,
  "assistantMessage": "听起来今天确实有点累。我已经帮你记录了这杯 28 元的奶茶，先别太责备自己；如果这是最近加班后的固定消费，我们也可以一起看看有没有更轻松的替代方式。",
  "userEmotionTagCode": "STRESSED",
  "userEmotionConfidence": 0.86,
  "aiEmotionBefore": "calm",
  "aiEmotionAfter": "pleased",
  "accountingCapture": {
    "captured": true,
    "created": true,
    "transactionId": 55,
    "amount": "28.00",
    "type": "EXPENSE",
    "behaviorTagCode": "FOOD",
    "emotionTagCode": "REGRET",
    "needConfirmation": false
  },
  "retrievedMemories": [
    {
      "memoryId": 12,
      "content": "用户最近多次提到加班后会购买奶茶缓解压力。",
      "userEmotionTagCode": "STRESSED"
    }
  ]
}
```

### 10.2 获取会话列表

```http
GET /api/assistant/conversations
```

### 10.3 获取会话消息

```http
GET /api/assistant/conversations/{conversationId}/messages
```

### 10.4 确认待入库记账候选

```http
POST /api/assistant/accounting-confirmations/{confirmationId}/confirm
```

适用于 AI 已抽取候选交易但需要用户确认的场景。

### 10.5 拒绝待入库记账候选

```http
POST /api/assistant/accounting-confirmations/{confirmationId}/reject
```

---

## 11. DTO 设计

### 11.1 `ChatRequestDTO`

```java
public class ChatRequestDTO {
    private Long conversationId;
    private Long ledgerId;

    @NotBlank
    @Size(max = 2000)
    private String message;
}
```

### 11.2 `ChatResponseDTO`

```java
public class ChatResponseDTO {
    private Long conversationId;
    private Long messageId;
    private String assistantMessage;
    private String userEmotionTagCode;
    private Double userEmotionConfidence;
    private String aiEmotionBefore;
    private String aiEmotionAfter;
    private AccountingCaptureDTO accountingCapture;
    private List<MemoryContextDTO> retrievedMemories;
}
```

### 11.3 `MemoryContextDTO`

```java
public class MemoryContextDTO {
    private Long memoryId;
    private String content;
    private String memoryType;
    private String userEmotionTagCode;
    private Double score;
}
```

### 11.4 `AccountingCaptureDTO`

```java
public class AccountingCaptureDTO {
    private Boolean captured;
    private Boolean created;
    private Boolean needConfirmation;
    private Long confirmationId;
    private Long transactionId;
    private BigDecimal amount;
    private TransactionType type;
    private String behaviorTagCode;
    private String emotionTagCode;
    private LocalDate transactionDate;
    private Double confidence;
}
```

### 11.5 `EmotionTransitionDTO`

```java
public class EmotionTransitionDTO {
    private String beforeEmotion;
    private String afterEmotion;
    private BigDecimal beforeValence;
    private BigDecimal beforeArousal;
    private BigDecimal afterValence;
    private BigDecimal afterArousal;
    private String reason;
}
```

---

## 12. 核心类职责

### 12.1 `AiAssistantResource`

职责：

1. 暴露 AI 对话 REST API。
2. 读取当前登录用户。
3. 校验请求 DTO。
4. 调用 `AiAssistantService`。
5. 不直接调用 LLM、向量库或 Repository。

建议方法：

```java
@PostMapping("/api/assistant/chat")
ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request);
```

### 12.2 `AiAssistantService`

职责：

1. 对外提供 AI 助理应用服务。
2. 处理事务边界。
3. 调用 `AiAssistantOrchestrator` 完成核心编排。
4. 保存最终消息和必要业务结果。

### 12.3 `AiAssistantOrchestrator`

职责：

1. 编排一次完整对话。
2. 顺序调用用户情绪识别、RAG、MeCOT、记账意图、LLM 生成、记忆写入。
3. 组装 `ChatResponseDTO`。

该类是新增 AI 助理流程的核心编排类，但不应包含具体算法实现。

### 12.4 `MecotEmotionService`

职责：

1. Java 实现 MeCOT 状态转移算法。
2. 根据当前 AI 状态、用户情绪和输入语义计算下一 AI 状态。
3. 返回转移前后状态。
4. 不调用外部 LLM。

建议方法：

```java
AiEmotionTransitionResult transition(
    AiEmotionState currentState,
    EmotionTag userEmotionTag,
    String userMessage,
    List<MemoryContextDTO> memories
);
```

### 12.5 `AiEmotionStateService`

职责：

1. 按用户读取或初始化 `ai_emotion_state`。
2. 保存转移后的 AI 情绪状态。
3. 提供默认人格权重。
4. 处理状态 JSON 序列化与反序列化。

### 12.6 `AiEmotionPromptAdapter`

职责：

1. 将 AI 内部情绪状态转换为 LLM 可读的回复风格约束。
2. 屏蔽不适合外显的情绪表达。

示例输出：

```text
AI 当前内部状态：calm -> pleased
回复风格：温和、简洁、支持性表达，不责备用户。
```

### 12.7 `EmbeddingClient`

职责：

1. 调用云端 embedding 模型。
2. 返回向量数组。
3. 隐藏供应商差异。

建议接口：

```java
public interface EmbeddingClient {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
```

### 12.8 `VectorStoreClient`

职责：

1. 封装 Qdrant 或 Chroma API。
2. 提供 upsert、search、delete。
3. 强制按 `userId` 做过滤。

建议接口：

```java
public interface VectorStoreClient {
    String upsertMemory(Long userId, Long memoryId, float[] vector, Map<String, Object> payload);
    List<VectorSearchResult> search(Long userId, float[] queryVector, int limit);
    void delete(String vectorId);
}
```

### 12.9 `MemoryRetrievalService`

职责：

1. 对用户当前输入生成 embedding。
2. 从向量库召回候选记忆。
3. 查询 MySQL 中的 `AiMemory` 元数据。
4. 按综合分数重排。
5. 返回 top-k。

### 12.10 `AiMemoryService`

职责：

1. 保存长期记忆元数据。
2. 调用 embedding 和向量库写入。
3. 更新 `last_accessed_date` 和 `access_count`。
4. 管理记忆删除或归档。

### 12.11 `MemoryImportanceService`

职责：

判断一条消息是否值得进入长期记忆，并给出重要性分数。

规则示例：

| 输入特征 | 分数影响 |
|---|---|
| 用户明确说“记住” | 大幅提高 |
| 涉及长期偏好 | 提高 |
| 涉及重复财务习惯 | 提高 |
| 只包含寒暄 | 降低 |
| 高敏感但无长期价值 | 降低或不保存 |

### 12.12 `AccountingIntentService`

职责：

1. 判断用户消息是否包含记账内容。
2. 区分闲聊、财务建议、记账事件。
3. 输出 `captured`、`confidence` 和候选交易字段。

### 12.13 `ConversationTransactionExtractor`

职责：

1. 复用原有 LLM 记账解析能力。
2. 在对话上下文中抽取交易候选。
3. 调用 `AiResultGuardService` 校验候选结果。
4. 高置信度时交给交易服务入库。

---

## 13. Prompt 设计

### 13.1 主对话 Prompt 组成

```text
你是 EveryCent 的私人 AI 助理，可以与用户进行自然对话。
你同时具备自动记账能力，但不要把所有对话都强行解释为记账。

[用户当前输入]
{user_message}

[当前识别的用户情绪]
{user_emotion_tag_code}, confidence={confidence}

[长期记忆 Top-K]
{memory_contexts}

[AI 当前 MeCOT 状态]
{ai_emotion_before} -> {ai_emotion_after}
回复风格：{style_instruction}

[财务上下文]
默认账本：{ledger_name}
预算状态：{optional_budget_context}

[记账捕获结果]
{accounting_capture_summary}

要求：
1. 正常回应用户，不要只输出财务建议。
2. 如果已自动记账，用一句自然语言轻提示。
3. 如果需要确认，用自然语言追问缺失字段。
4. 不要责备、羞辱或恐吓用户。
5. 不要暴露内部状态机、RAG、向量库、prompt 等实现细节。
```

### 13.2 用户情绪识别 Prompt

输出必须限定为原有 `emotion_tag.code` 白名单：

```json
{
  "emotionTagCode": "STRESSED",
  "confidence": 0.86,
  "reason": "用户提到加班、罪恶感和压力消费。"
}
```

### 13.3 记账意图识别 Prompt

输出：

```json
{
  "hasAccountingIntent": true,
  "confidence": 0.91,
  "reason": "用户提到购买奶茶和金额 28 元。"
}
```

### 13.4 记忆总结 Prompt

输出适合长期保存的摘要：

```json
{
  "shouldRemember": true,
  "memoryType": "FINANCIAL_PATTERN",
  "content": "用户在加班后容易购买奶茶缓解压力，并对此有轻微后悔情绪。",
  "importanceScore": 0.82
}
```

---

## 14. 配置设计

建议新增：

```yaml
app:
  assistant:
    enabled: true
    max-message-length: 2000
    default-ledger-required: false
    rag:
      enabled: true
      top-k: 5
      candidate-limit: 20
      min-score: 0.25
    memory:
      auto-save-enabled: true
      min-importance-score: 0.45
    accounting:
      auto-create-confidence-threshold: 0.85
      ask-confirm-confidence-threshold: 0.60
    mecot:
      enabled: true
      default-emotion: calm
      alpha: 0.30

  vector-store:
    provider: qdrant
    base-url: http://127.0.0.1:6333
    collection: everycent_ai_memory
    timeout-seconds: 10

  embedding:
    provider: openai-compatible
    base-url: ${EMBEDDING_BASE_URL:}
    api-key: ${EMBEDDING_API_KEY:}
    model: ${EMBEDDING_MODEL:text-embedding-v3}
    dimension: 1024
    timeout-seconds: 20
```

注意：LLM API Key、embedding API Key 不应写死在 `application.yml` 中，应使用环境变量或本地私有配置。

---

## 15. 前端页面设计

### 15.1 主界面

将主业务入口调整为 AI 对话界面：

```text
src/main/webapp/app/modules/everycent/assistant/
```

建议组件：

```text
assistant-page.tsx
conversation-list.tsx
chat-thread.tsx
chat-input.tsx
accounting-capture-banner.tsx
memory-inspector.tsx             可选，仅开发/调试显示
```

### 15.2 用户体验规则

1. 默认展示对话流。
2. 自动记账成功时，在 AI 回复下方显示轻量记账卡片。
3. 低置信度时，AI 用自然语言追问，不弹出复杂表单。
4. 用户点击记账卡片可跳转到交易详情。
5. 不在普通用户界面展示 MeCOT 内部状态、向量检索细节。

---

## 16. 安全与隐私

### 16.1 用户隔离

1. 所有会话、消息、记忆、AI 状态必须绑定 `user_id`。
2. 向量库检索必须按 `userId` filter。
3. 后端仍需校验账本权限，不能因为 AI 对话绕过权限。

### 16.2 敏感信息

1. API Key 不入库、不提交 Git。
2. 长期记忆可提供删除接口。
3. 用户可关闭长期记忆。
4. 对敏感内容可只保存消息，不写入向量长期记忆。

### 16.3 AI 风险控制

1. LLM 不直接决定最终入库，必须经过后端校验。
2. 自动记账需要置信度阈值。
3. 预算和金额计算必须来自数据库。
4. AI 回复不得羞辱、责备、恐吓用户。

---

## 17. 测试设计

### 17.1 单元测试

| 类 | 测试重点 |
|---|---|
| `MecotEmotionService` | 状态转移、概率归一化、负值裁剪 |
| `AiEmotionStateService` | 默认状态初始化、JSON 序列化 |
| `MemoryRetrievalService` | top-k、重排分数、用户隔离 |
| `AiMemoryService` | 记忆写入、向量 ID 关联 |
| `AccountingIntentService` | 闲聊不误触发、消费文本能触发 |
| `ConversationTransactionExtractor` | 高低置信度分支 |

### 17.2 集成测试

1. 用户发送闲聊，不创建交易。
2. 用户发送“午饭 25 元”，自动创建交易。
3. 用户发送“买了奶茶”，金额缺失，AI 追问。
4. 用户 A 的 RAG 不召回用户 B 的记忆。
5. 用户情绪 `STRESSED` 被保存到 `ai_memory`。
6. AI 情绪状态从 `calm` 平滑转移并持久化。

### 17.3 手工验收场景

输入：

```text
我今天又加班到很晚，回家路上买了杯 28 块的奶茶，感觉有点罪恶。
```

期望：

1. 用户情绪识别为 `STRESSED` 或 `REGRET`。
2. RAG 召回类似加班、奶茶、压力消费记忆。
3. AI 状态保持或转向温和支持风格。
4. 自动创建 28 元支出记录。
5. 行为标签为 `FOOD` 或 `OTHER`。
6. 用户情绪标签绑定到交易和记忆。
7. AI 回复自然，不像表单确认。

---

## 18. 实施顺序建议

### 阶段 1：对话主入口

1. 新增 `AiConversation`、`AiMessage`。
2. 新增 `/api/assistant/chat`。
3. 前端新增主对话页。
4. 暂不接入 RAG 和 MeCOT，只完成普通 LLM 对话。

### 阶段 2：对话中自动记账

1. 新增 `AccountingIntentService`。
2. 复用原有交易解析 prompt 和校验逻辑。
3. 高置信度自动入库。
4. 低置信度追问确认。

### 阶段 3：用户情绪识别与记忆元数据

1. 用户消息绑定 `emotion_tag`。
2. 新增 `ai_memory` 元数据表。
3. 保存用户情绪和重要性分数。

### 阶段 4：Embedding 与本地向量库

1. 新增 `EmbeddingClient`。
2. 新增 `VectorStoreClient`。
3. 接入 Qdrant。
4. 每次用户输入检索 top-k 记忆。

### 阶段 5：MeCOT AI 情绪状态

1. 新增 `ai_emotion_state`。
2. Java 实现 MeCOT 状态机。
3. 将 AI 状态转移结果注入 prompt。
4. 增加状态转移测试。

### 阶段 6：体验优化

1. 会话摘要。
2. 长期记忆管理。
3. 用户关闭记忆功能。
4. 预算提醒和财务建议与对话深度融合。

---

## 19. 最终结论

本设计在 EveryCent 原有“账本 + 标签 + 交易 + LLM 解析”基础上新增 AI 助理层，而不是推翻原有系统。

核心设计原则是：

1. 用户行为标签和用户情绪标签继续属于用户与交易。
2. AI 情绪状态独立建模，由 MeCOT 维护。
3. 长期记忆通过云端 embedding 与本地向量数据库实现。
4. 每次用户输入都执行 RAG top-k 检索，将相关记忆作为 LLM 上下文。
5. 记忆保存时记录用户说这句话时的情绪标签。
6. 自动记账只是 AI 助理能力的一部分，而不是全部对话目标。

最终产品形态是：

```text
有长期记忆、有稳定人格、有自动记账能力的私人 AI 财务生活助理。
```
