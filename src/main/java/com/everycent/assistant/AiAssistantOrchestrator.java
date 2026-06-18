package com.everycent.assistant;

import com.everycent.assistant.dto.AccountingCaptureDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.assistant.dto.MemoryContextDTO;
import com.everycent.assistant.memory.AiMemoryService;
import com.everycent.assistant.memory.MemoryRetrievalService;
import com.everycent.assistant.memory.VectorSearchResult;
import com.everycent.assistant.prompt.AssistantPromptBuilder;
import com.everycent.domain.User;
import com.everycent.llm.client.LlmClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiAssistantOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AiAssistantOrchestrator.class);
    private static final int MEMORY_TOP_K = 5;
    private static final String INITIAL_USER_EMOTION_STATE = "中立";

    private final MemoryRetrievalService memoryRetrievalService;
    private final AiMemoryService aiMemoryService;
    private final AssistantPromptBuilder assistantPromptBuilder;
    private final LlmClient llmClient;

    public AiAssistantOrchestrator(
        MemoryRetrievalService memoryRetrievalService,
        AiMemoryService aiMemoryService,
        AssistantPromptBuilder assistantPromptBuilder,
        LlmClient llmClient
    ) {
        this.memoryRetrievalService = memoryRetrievalService;
        this.aiMemoryService = aiMemoryService;
        this.assistantPromptBuilder = assistantPromptBuilder;
        this.llmClient = llmClient;
    }

    public ChatResponseDTO chat(User currentUser, ChatRequestDTO request) {
        Long userId = requireUserId(currentUser);
        String userMessage = requireMessage(request);
        Long conversationId = request.getConversationId() == null ? System.currentTimeMillis() : request.getConversationId();
        Long messageId = System.nanoTime();

        List<MemoryContextDTO> memories = retrieveMemories(userId, userMessage);
        String prompt = assistantPromptBuilder.buildSingleTurnPrompt(userMessage, INITIAL_USER_EMOTION_STATE, memories);
        LOG.info("Assistant prompt built for userId={}, conversationId={}, memoryCount={}", userId, conversationId, memories.size());

        String assistantMessage = llmClient.complete(prompt);
        Long memoryId = Math.abs((conversationId + ":" + messageId).hashCode()) + System.currentTimeMillis();
        String memoryContent = "用户说：" + userMessage + "\n皓尾回复：" + assistantMessage;
        String vectorId = aiMemoryService.saveMemoryVector(
            userId,
            memoryId,
            memoryContent,
            Map.of(
                "userId",
                userId,
                "conversationId",
                conversationId,
                "messageId",
                messageId,
                "memoryId",
                memoryId,
                "memoryType",
                "CONVERSATION_TURN",
                "userEmotionTagCode",
                "NEUTRAL",
                "createdDate",
                Instant.now().toString(),
                "content",
                memoryContent
            )
        );
        LOG.info("Assistant conversation memory saved userId={}, memoryId={}, vectorId={}", userId, memoryId, vectorId);

        ChatResponseDTO response = new ChatResponseDTO();
        response.setConversationId(conversationId);
        response.setMessageId(messageId);
        response.setAssistantMessage(assistantMessage);
        response.setUserEmotionTagCode("NEUTRAL");
        response.setUserEmotionConfidence(1.0);
        response.setAiEmotionBefore("neutral");
        response.setAiEmotionAfter("neutral");
        response.setAccountingCapture(noAccountingCapture());
        response.setRetrievedMemories(memories);
        return response;
    }

    private List<MemoryContextDTO> retrieveMemories(Long userId, String userMessage) {
        try {
            return memoryRetrievalService.searchUserMemory(userId, userMessage, MEMORY_TOP_K).stream().map(this::toMemoryContext).toList();
        } catch (RuntimeException e) {
            LOG.warn("User memory retrieval failed, continuing without memory. userId={}, reason={}", userId, e.getMessage());
            return List.of();
        }
    }

    private MemoryContextDTO toMemoryContext(VectorSearchResult result) {
        Map<String, Object> payload = result.getPayload();
        MemoryContextDTO dto = new MemoryContextDTO();
        dto.setScore(result.getScore());
        dto.setMemoryId(asLong(payload == null ? null : payload.get("memoryId")));
        dto.setContent(asString(payload == null ? null : payload.get("content")));
        dto.setMemoryType(asString(payload == null ? null : payload.get("memoryType")));
        dto.setUserEmotionTagCode(asString(payload == null ? null : payload.get("userEmotionTagCode")));
        return dto;
    }

    private AccountingCaptureDTO noAccountingCapture() {
        AccountingCaptureDTO dto = new AccountingCaptureDTO();
        dto.setCaptured(false);
        dto.setCreated(false);
        dto.setNeedConfirmation(false);
        return dto;
    }

    private Long requireUserId(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("当前用户不能为空");
        }
        return currentUser.getId();
    }

    private String requireMessage(ChatRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("message 不能为空");
        }
        return request.getMessage();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
