package com.everycent.assistant;

import com.everycent.assistant.dto.AccountingCaptureDTO;
import com.everycent.assistant.dto.ChatHistoryMessageDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.assistant.memory.AiMemoryService;
import com.everycent.assistant.memory.MemoryRetrievalService;
import com.everycent.assistant.prompt.AssistantPromptBuilder;
import com.everycent.assistant.validation.DialogueScene;
import com.everycent.assistant.validation.DialogueSceneClassifier;
import com.everycent.assistant.AssistantReplyPostProcessor;
import com.everycent.domain.User;
import com.everycent.llm.client.LlmClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiAssistantOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AiAssistantOrchestrator.class);
    private static final String INITIAL_USER_EMOTION_STATE = "中立";

    private final MemoryRetrievalService memoryRetrievalService;
    private final AiMemoryService aiMemoryService;
    private final AssistantPromptBuilder assistantPromptBuilder;
    private final DialogueSceneClassifier dialogueSceneClassifier;
    private final AssistantReplyPostProcessor replyPostProcessor;
    private final LlmClient llmClient;
    private final SimpleChatReplyService simpleChatReplyService;

    public AiAssistantOrchestrator(
        MemoryRetrievalService memoryRetrievalService,
        AiMemoryService aiMemoryService,
        AssistantPromptBuilder assistantPromptBuilder,
        DialogueSceneClassifier dialogueSceneClassifier,
        AssistantReplyPostProcessor replyPostProcessor,
        LlmClient llmClient,
        SimpleChatReplyService simpleChatReplyService
    ) {
        this.memoryRetrievalService = memoryRetrievalService;
        this.aiMemoryService = aiMemoryService;
        this.assistantPromptBuilder = assistantPromptBuilder;
        this.dialogueSceneClassifier = dialogueSceneClassifier;
        this.replyPostProcessor = replyPostProcessor;
        this.llmClient = llmClient;
        this.simpleChatReplyService = simpleChatReplyService;
    }

    public ChatResponseDTO chat(User currentUser, ChatRequestDTO request) {
        return chat(currentUser, request, List.of());
    }

    public ChatResponseDTO chat(User currentUser, ChatRequestDTO request, List<ChatHistoryMessageDTO> conversationHistory) {
        Long userId = requireUserId(currentUser);
        String userMessage = requireMessage(request);
        Long conversationId = request.getConversationId() == null ? System.currentTimeMillis() : request.getConversationId();
        Long messageId = System.nanoTime();

        DialogueScene scene = dialogueSceneClassifier.classify(userMessage);
        String assistantMessage = simpleChatReplyService.reply(userMessage);
        if (assistantMessage == null) {
            String prompt = assistantPromptBuilder.buildSingleTurnPrompt(
                userMessage,
                INITIAL_USER_EMOTION_STATE,
                List.of(),
                conversationHistory
            );
            LOG.info("Assistant prompt built for userId={}, conversationId={}, rag=false, memoryPersist=false, mecot=false", userId, conversationId);

            String rawReply = llmClient.complete(prompt);
            assistantMessage = replyPostProcessor.process(userMessage, rawReply, scene);
        }

        ChatResponseDTO response = new ChatResponseDTO();
        response.setConversationId(conversationId);
        response.setMessageId(messageId);
        response.setAssistantMessage(assistantMessage);
        response.setUserEmotionTagCode("NEUTRAL");
        response.setUserEmotionConfidence(1.0);
        response.setAiEmotionBefore("calm");
        response.setAiEmotionAfter("calm");
        response.setAccountingCapture(noAccountingCapture());
        response.setRetrievedMemories(List.of());
        return response;
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

}
