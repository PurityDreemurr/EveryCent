package com.everycent.assistant;

import com.everycent.assistant.dto.AssistantResponseCardDTO;
import com.everycent.assistant.dto.ChatHistoryDTO;
import com.everycent.assistant.dto.ChatHistoryMessageDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.domain.AiConversation;
import com.everycent.domain.AiMessage;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.repository.AiConversationRepository;
import com.everycent.repository.AiMessageRepository;
import com.everycent.service.LedgerPermissionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AssistantConversationStore {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final TypeReference<List<AssistantResponseCardDTO>> CARD_LIST_TYPE = new TypeReference<>() {};
    private static final Set<String> ALLOWED_AI_EMOTIONS = Set.of(
        "surprised",
        "happy",
        "pleased",
        "fearful",
        "angry",
        "grieved",
        "sad",
        "disgusted",
        "depressed",
        "tired",
        "calm",
        "relieved"
    );

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final LedgerPermissionService ledgerPermissionService;
    private final ObjectMapper objectMapper;

    public AssistantConversationStore(
        AiConversationRepository conversationRepository,
        AiMessageRepository messageRepository,
        LedgerPermissionService ledgerPermissionService,
        ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ledgerPermissionService = ledgerPermissionService;
        this.objectMapper = objectMapper;
    }

    public Long ensureConversation(User user, ChatRequestDTO request) {
        if (user == null || request == null || request.getLedgerId() == null) {
            return null;
        }
        Ledger ledger = readableLedger(user, request.getLedgerId());
        AiConversation conversation = findConversation(user, ledger, request.getConversationId());
        if (conversation == null) {
            conversation = newConversation(user, ledger, request.getMessage());
        }
        return conversationRepository.save(conversation).getId();
    }

    public Long appendExchange(User user, ChatRequestDTO request, ChatResponseDTO response) {
        if (user == null || request == null || response == null || request.getLedgerId() == null || response.getConversationId() == null) {
            return response == null ? null : response.getMessageId();
        }
        Ledger ledger = readableLedger(user, request.getLedgerId());
        AiConversation conversation = findConversation(user, ledger, response.getConversationId());
        if (conversation == null) {
            conversation = newConversation(user, ledger, request.getMessage());
        }

        Instant now = Instant.now();
        messageRepository.save(userMessage(conversation, user, ledger, request.getMessage(), now));
        AiMessage assistantMessage = messageRepository.save(assistantMessage(conversation, user, ledger, response, now));

        conversation.setLastMessageDate(now);
        conversation.setLastModifiedDate(now);
        conversationRepository.save(conversation);
        return assistantMessage.getId();
    }

    @Transactional(readOnly = true)
    public ChatHistoryDTO latestHistory(User user, Long ledgerId) {
        ChatHistoryDTO history = new ChatHistoryDTO();
        history.setLedgerId(ledgerId);
        if (user == null || ledgerId == null) {
            return history;
        }
        Ledger ledger = readableLedger(user, ledgerId);
        return conversationRepository
            .findFirstByUserAndLedgerAndArchivedFalseOrderByLastMessageDateDescIdDesc(user, ledger)
            .map(conversation -> toHistory(conversation, ledgerId))
            .orElse(history);
    }

    private AiConversation findConversation(User user, Ledger ledger, Long conversationId) {
        if (conversationId != null) {
            return conversationRepository.findOneByIdAndUserAndLedgerAndArchivedFalse(conversationId, user, ledger).orElse(null);
        }
        return conversationRepository.findFirstByUserAndLedgerAndArchivedFalseOrderByLastMessageDateDescIdDesc(user, ledger).orElse(null);
    }

    private AiConversation newConversation(User user, Ledger ledger, String message) {
        Instant now = Instant.now();
        AiConversation conversation = new AiConversation();
        conversation.setUser(user);
        conversation.setLedger(ledger);
        conversation.setTitle(title(message));
        conversation.setCreatedDate(now);
        conversation.setLastMessageDate(now);
        conversation.setLastModifiedDate(now);
        conversation.setArchived(false);
        return conversation;
    }

    private AiMessage userMessage(AiConversation conversation, User user, Ledger ledger, String content, Instant now) {
        AiMessage message = baseMessage(conversation, user, ledger, "USER", content, now);
        message.setMessageType("CHAT");
        return message;
    }

    private AiMessage assistantMessage(AiConversation conversation, User user, Ledger ledger, ChatResponseDTO response, Instant now) {
        AiMessage message = baseMessage(conversation, user, ledger, "ASSISTANT", response.getAssistantMessage(), now);
        message.setMessageType("CHAT");
        message.setResponseType(response.getResponseType());
        message.setCardsJson(json(response.getCards()));
        message.setSkillResultsJson(json(response.getSkillResults()));
        message.setAccountingCaptureJson(json(response.getAccountingCapture()));
        message.setAiEmotionBefore(databaseEmotion(response.getAiEmotionBefore()));
        message.setAiEmotionAfter(databaseEmotion(response.getAiEmotionAfter()));
        return message;
    }

    private AiMessage baseMessage(AiConversation conversation, User user, Ledger ledger, String role, String content, Instant now) {
        AiMessage message = new AiMessage();
        message.setConversation(conversation);
        message.setUser(user);
        message.setLedger(ledger);
        message.setRole(role);
        message.setContent(StringUtils.hasText(content) ? content : "");
        message.setCreatedDate(now);
        return message;
    }

    private ChatHistoryDTO toHistory(AiConversation conversation, Long ledgerId) {
        ChatHistoryDTO history = new ChatHistoryDTO();
        history.setConversationId(conversation.getId());
        history.setLedgerId(ledgerId);
        history.setMessages(messageRepository.findAllByConversationOrderByCreatedDateAscIdAsc(conversation).stream().map(this::toMessageDTO).toList());
        return history;
    }

    private ChatHistoryMessageDTO toMessageDTO(AiMessage message) {
        ChatHistoryMessageDTO dto = new ChatHistoryMessageDTO();
        dto.setId(String.valueOf(message.getId()));
        dto.setRole("ASSISTANT".equals(message.getRole()) ? "assistant" : "user");
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedDate() == null ? null : message.getCreatedDate().toString());
        dto.setCards(cards(message.getCardsJson()));
        return dto;
    }

    private Ledger readableLedger(User user, Long ledgerId) {
        ledgerPermissionService.checkReadPermission(user, ledgerId);
        return ledgerPermissionService.getLedgerOrThrow(ledgerId);
    }

    private String title(String message) {
        if (!StringUtils.hasText(message)) {
            return "新对话";
        }
        String normalized = message.trim().replaceAll("\\s+", " ");
        return normalized.length() <= TITLE_MAX_LENGTH ? normalized : normalized.substring(0, TITLE_MAX_LENGTH);
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String databaseEmotion(String emotion) {
        if (!StringUtils.hasText(emotion)) {
            return null;
        }
        String normalized = emotion.trim().toLowerCase();
        if ("neutral".equals(normalized) || "中立".equals(normalized)) {
            return "calm";
        }
        return ALLOWED_AI_EMOTIONS.contains(normalized) ? normalized : "calm";
    }

    private List<AssistantResponseCardDTO> cards(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, CARD_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
