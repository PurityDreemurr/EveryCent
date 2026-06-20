package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.assistant.dto.AssistantResponseCardDTO;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.everycent.domain.AiConversation;
import com.everycent.domain.AiMessage;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.repository.AiConversationRepository;
import com.everycent.repository.AiMessageRepository;
import com.everycent.service.LedgerPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AssistantConversationStoreTest {

    private final AiConversationRepository conversationRepository = org.mockito.Mockito.mock(AiConversationRepository.class);
    private final AiMessageRepository messageRepository = org.mockito.Mockito.mock(AiMessageRepository.class);
    private final LedgerPermissionService ledgerPermissionService = org.mockito.Mockito.mock(LedgerPermissionService.class);
    private final AssistantConversationStore store = new AssistantConversationStore(
        conversationRepository,
        messageRepository,
        ledgerPermissionService,
        new ObjectMapper()
    );

    @Test
    void shouldCreateConversationForUserAndLedger() {
        User user = user();
        Ledger ledger = ledger();
        ChatRequestDTO request = request("午饭28", 10L, null);
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(conversationRepository.findFirstByUserAndLedgerAndArchivedFalseOrderByLastMessageDateDescIdDesc(user, ledger))
            .thenReturn(Optional.empty());
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            conversation.setId(99L);
            return conversation;
        });

        Long conversationId = store.ensureConversation(user, request);

        assertThat(conversationId).isEqualTo(99L);
        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getLedger()).isEqualTo(ledger);
        assertThat(captor.getValue().getTitle()).isEqualTo("午饭28");
    }

    @Test
    void shouldAppendUserAndAssistantMessages() {
        User user = user();
        Ledger ledger = ledger();
        AiConversation conversation = conversation(user, ledger);
        ChatRequestDTO request = request("午饭28", 10L, 99L);
        ChatResponseDTO response = response(99L);
        AtomicLong ids = new AtomicLong(100L);
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(conversationRepository.findOneByIdAndUserAndLedgerAndArchivedFalse(99L, user, ledger)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            message.setId(ids.getAndIncrement());
            return message;
        });
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Long assistantMessageId = store.appendExchange(user, request, response);

        assertThat(assistantMessageId).isEqualTo(101L);
        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo("USER");
        assertThat(messageCaptor.getAllValues().get(0).getContent()).isEqualTo("午饭28");
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(messageCaptor.getAllValues().get(1).getCardsJson()).contains("transaction_created");
        assertThat(messageCaptor.getAllValues().get(1).getAiEmotionBefore()).isEqualTo("calm");
        assertThat(messageCaptor.getAllValues().get(1).getAiEmotionAfter()).isEqualTo("calm");
    }

    @Test
    void shouldLoadLatestHistoryByUserAndLedger() {
        User user = user();
        Ledger ledger = ledger();
        AiConversation conversation = conversation(user, ledger);
        AiMessage userMessage = message(conversation, user, ledger, 100L, "USER", "午饭28", null);
        AiMessage assistantMessage = message(
            conversation,
            user,
            ledger,
            101L,
            "ASSISTANT",
            "已记账。",
            "[{\"type\":\"transaction_created\",\"title\":\"记账成功\",\"message\":\"这笔记录已经入账。\"}]"
        );
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(conversationRepository.findFirstByUserAndLedgerAndArchivedFalseOrderByLastMessageDateDescIdDesc(user, ledger))
            .thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationOrderByCreatedDateAscIdAsc(conversation)).thenReturn(List.of(userMessage, assistantMessage));

        var history = store.latestHistory(user, 10L);

        assertThat(history.getConversationId()).isEqualTo(99L);
        assertThat(history.getLedgerId()).isEqualTo(10L);
        assertThat(history.getMessages()).hasSize(2);
        assertThat(history.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(history.getMessages().get(1).getRole()).isEqualTo("assistant");
        assertThat(history.getMessages().get(1).getCards()).hasSize(1);
        assertThat(history.getMessages().get(1).getCards().get(0).getType()).isEqualTo("transaction_created");
    }

    @Test
    void shouldArchiveCurrentUserLedgerConversationsWhenClearingHistory() {
        User user = user();
        Ledger ledger = ledger();
        AiConversation conversation = conversation(user, ledger);
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(conversationRepository.findAllByUserAndLedgerAndArchivedFalse(user, ledger)).thenReturn(List.of(conversation));

        store.clearHistory(user, 10L);

        assertThat(conversation.getArchived()).isTrue();
        assertThat(conversation.getLastModifiedDate()).isNotNull();
        verify(conversationRepository).saveAll(List.of(conversation));
    }

    @Test
    void shouldReturnEmptyHistoryAfterClearedConversationIsArchived() {
        User user = user();
        Ledger ledger = ledger();
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(conversationRepository.findFirstByUserAndLedgerAndArchivedFalseOrderByLastMessageDateDescIdDesc(user, ledger))
            .thenReturn(Optional.empty());

        var history = store.latestHistory(user, 10L);

        assertThat(history.getConversationId()).isNull();
        assertThat(history.getMessages()).isEmpty();
    }

    private ChatRequestDTO request(String message, Long ledgerId, Long conversationId) {
        ChatRequestDTO request = new ChatRequestDTO();
        request.setMessage(message);
        request.setLedgerId(ledgerId);
        request.setConversationId(conversationId);
        return request;
    }

    private ChatResponseDTO response(Long conversationId) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setConversationId(conversationId);
        response.setAssistantMessage("已记账。");
        response.setResponseType("transaction_created");
        response.setAiEmotionBefore("neutral");
        response.setAiEmotionAfter("neutral");
        response.setCards(List.of(new AssistantResponseCardDTO("transaction_created", "记账成功", "这笔记录已经入账。", null)));
        return response;
    }

    private AiConversation conversation(User user, Ledger ledger) {
        AiConversation conversation = new AiConversation();
        conversation.setId(99L);
        conversation.setUser(user);
        conversation.setLedger(ledger);
        conversation.setCreatedDate(Instant.now());
        conversation.setLastMessageDate(Instant.now());
        conversation.setArchived(false);
        return conversation;
    }

    private AiMessage message(
        AiConversation conversation,
        User user,
        Ledger ledger,
        Long id,
        String role,
        String content,
        String cardsJson
    ) {
        AiMessage message = new AiMessage();
        message.setId(id);
        message.setConversation(conversation);
        message.setUser(user);
        message.setLedger(ledger);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedDate(Instant.now());
        message.setCardsJson(cardsJson);
        return message;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setLogin("alice");
        return user;
    }

    private Ledger ledger() {
        Ledger ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("日常账本");
        return ledger;
    }
}
