package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.Budget;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.NotificationMessage;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.EmotionValence;
import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.client.LlmClient;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateRequestDTO;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.llm.parser.LlmJsonResponseParser;
import com.everycent.llm.prompt.AlertPromptBuilder;
import com.everycent.llm.prompt.TransactionPromptBuilder;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmParsingServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private BehaviorTagRepository behaviorTagRepository;

    @Mock
    private EmotionTagRepository emotionTagRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @Mock
    private NotificationMessageRepository notificationMessageRepository;

    @Mock
    private UserLedgerPermissionRepository userLedgerPermissionRepository;

    @Mock
    private TransactionPromptBuilder transactionPromptBuilder;

    @Mock
    private AlertPromptBuilder alertPromptBuilder;

    @Mock
    private LlmClient llmClient;

    @Mock
    private LlmJsonResponseParser parser;

    @Mock
    private AiResultGuardService guardService;

    private LlmParsingService service;
    private User currentUser;
    private Ledger ledger;

    @BeforeEach
    void setUp() {
        service =
            new LlmParsingService(
                ledgerRepository,
                behaviorTagRepository,
                emotionTagRepository,
                budgetRepository,
                transactionRecordRepository,
                notificationMessageRepository,
                userLedgerPermissionRepository,
                transactionPromptBuilder,
                alertPromptBuilder,
                llmClient,
                parser,
                guardService
            );
        currentUser = new User();
        currentUser.setId(1L);
        ledger = new Ledger().name("测试账本").createdDate(Instant.now()).creator(currentUser);
        ledger.setId(10L);
    }

    @Test
    void parseTransactionShouldOrchestratePromptClientParserAndGuard() {
        TransactionParseRequestDTO request = new TransactionParseRequestDTO();
        request.setLedgerId(10L);
        request.setText("今天午饭 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));
        BehaviorTag behaviorTag = new BehaviorTag().code("FOOD").name("餐饮");
        EmotionTag emotionTag = new EmotionTag().code("HAPPY").name("开心").valence(EmotionValence.POSITIVE);
        TransactionParseResultDTO parsed = new TransactionParseResultDTO();
        TransactionParseResultDTO validated = new TransactionParseResultDTO();
        validated.setBehaviorTagName("餐饮");

        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(behaviorTagRepository.findAll()).thenReturn(List.of(behaviorTag));
        when(emotionTagRepository.findAll()).thenReturn(List.of(emotionTag));
        when(transactionPromptBuilder.build(request.getText(), ledger, List.of(behaviorTag), List.of(emotionTag), request.getTransactionDate()))
            .thenReturn("prompt");
        when(llmClient.complete("prompt")).thenReturn("{json}");
        when(parser.parseTransaction("{json}")).thenReturn(parsed);
        when(guardService.validateTransactionResult(parsed, 10L, currentUser)).thenReturn(validated);

        TransactionParseResultDTO result = service.parseTransaction(request, currentUser);

        assertThat(result).isSameAs(validated);
        assertThat(parsed.getRawInput()).isEqualTo("今天午饭 50 元");
        verify(llmClient).complete("prompt");
        verify(parser).parseTransaction("{json}");
        verify(guardService).validateTransactionResult(parsed, 10L, currentUser);
    }

    @Test
    void parseAndCreateTransactionShouldSaveValidatedNaturalLanguageRecord() {
        NaturalLanguageTransactionCreateRequestDTO request = new NaturalLanguageTransactionCreateRequestDTO();
        request.setText("今天午饭 50 元");
        request.setTransactionDate(LocalDate.of(2026, 6, 17));
        request.setConfirm(true);
        BehaviorTag behaviorTag = new BehaviorTag().code("FOOD").name("餐饮");
        EmotionTag emotionTag = new EmotionTag().code("HAPPY").name("开心").valence(EmotionValence.POSITIVE);
        TransactionParseResultDTO parsed = new TransactionParseResultDTO();
        TransactionParseResultDTO validated = new TransactionParseResultDTO();
        validated.setAmount(new BigDecimal("50.00"));
        validated.setType(TransactionType.EXPENSE);
        validated.setTransactionDate(LocalDate.of(2026, 6, 17));
        validated.setDescription("午饭");
        validated.setRawInput("今天午饭 50 元");
        validated.setBehaviorTagCode("FOOD");
        validated.setEmotionTagCode("HAPPY");

        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(behaviorTagRepository.findAll()).thenReturn(List.of(behaviorTag));
        when(emotionTagRepository.findAll()).thenReturn(List.of(emotionTag));
        when(transactionPromptBuilder.build(any(), any(), any(), any(), any())).thenReturn("prompt");
        when(llmClient.complete("prompt")).thenReturn("{json}");
        when(parser.parseTransaction("{json}")).thenReturn(parsed);
        when(guardService.validateTransactionResult(parsed, 10L, currentUser)).thenReturn(validated);
        when(behaviorTagRepository.findOneByCode("FOOD")).thenReturn(Optional.of(behaviorTag));
        when(emotionTagRepository.findOneByCode("HAPPY")).thenReturn(Optional.of(emotionTag));
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord record = invocation.getArgument(0);
            record.setId(101L);
            return record;
        });

        NaturalLanguageTransactionCreateResultDTO result = service.parseAndCreateTransaction(10L, request, currentUser);

        ArgumentCaptor<TransactionRecord> recordCaptor = ArgumentCaptor.forClass(TransactionRecord.class);
        verify(transactionRecordRepository).save(recordCaptor.capture());
        TransactionRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getSource()).isEqualTo(RecordSource.NATURAL_LANGUAGE);
        assertThat(savedRecord.getLedger()).isSameAs(ledger);
        assertThat(savedRecord.getCreator()).isSameAs(currentUser);
        assertThat(savedRecord.getBehaviorTag()).isSameAs(behaviorTag);
        assertThat(savedRecord.getEmotionTag()).isSameAs(emotionTag);
        assertThat(result.getTransactionId()).isEqualTo(101L);
        assertThat(result.getBehaviorTagName()).isEqualTo("餐饮");
        assertThat(result.getEmotionTagName()).isEqualTo("开心");
    }

    @Test
    void generateBudgetAlertShouldCalculateBudgetFactsInBackend() {
        AiAlertRequestDTO request = new AiAlertRequestDTO();
        request.setLedgerId(10L);
        request.setBudgetId(20L);
        Budget budget = new Budget()
            .cycle(BudgetCycle.MONTHLY)
            .periodStart(LocalDate.of(2026, 6, 1))
            .periodEnd(LocalDate.of(2026, 6, 30))
            .limitAmount(new BigDecimal("1000.00"))
            .alertThreshold(new BigDecimal("0.80"))
            .enabled(true)
            .ledger(ledger);
        budget.setId(20L);
        EmotionTag emotionTag = new EmotionTag().code("HAPPY").name("开心").valence(EmotionValence.POSITIVE);
        TransactionRecord expense = new TransactionRecord()
            .ledger(ledger)
            .type(TransactionType.EXPENSE)
            .amount(new BigDecimal("820.00"))
            .transactionDate(LocalDate.of(2026, 6, 15))
            .emotionTag(emotionTag);
        TransactionRecord income = new TransactionRecord()
            .ledger(ledger)
            .type(TransactionType.INCOME)
            .amount(new BigDecimal("100.00"))
            .transactionDate(LocalDate.of(2026, 6, 16));
        AiAlertResultDTO parsed = new AiAlertResultDTO();
        parsed.setTitle("预算提醒");
        parsed.setContent("预算已使用 82%。");
        parsed.setLevel(AiAlertResultDTO.AlertLevel.WARNING);
        parsed.setNeedNotification(true);

        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(budgetRepository.findById(20L)).thenReturn(Optional.of(budget));
        when(transactionRecordRepository.findAllByLedgerAndTransactionDateBetween(ledger, budget.getPeriodStart(), budget.getPeriodEnd()))
            .thenReturn(List.of(expense, income));
        when(transactionRecordRepository.findAllByLedgerAndTransactionDateBetween(ledger, budget.getPeriodEnd().minusDays(14), budget.getPeriodEnd()))
            .thenReturn(List.of(expense));
        when(alertPromptBuilder.build(any(), any(), any(), any())).thenReturn("alert-prompt");
        when(llmClient.complete("alert-prompt")).thenReturn("{alert-json}");
        when(parser.parseAlert("{alert-json}")).thenReturn(parsed);
        when(guardService.validateAlertResult(parsed)).thenReturn(parsed);

        AiAlertResultDTO result = service.generateBudgetAlert(request, currentUser);

        assertThat(result.getUsedAmount()).isEqualByComparingTo("820.00");
        assertThat(result.getLimitAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getUsedRatio()).isEqualByComparingTo("0.8200");
        assertThat(result.getOverBudget()).isFalse();
        assertThat(result.getNeedNotification()).isTrue();
        verify(alertPromptBuilder).build(any(), any(), any(), any());
        verify(parser).parseAlert("{alert-json}");
    }

    @Test
    void generateBudgetAlertShouldPersistNotificationWhenRequestedAndNeeded() {
        AiAlertRequestDTO request = new AiAlertRequestDTO();
        request.setLedgerId(10L);
        request.setBudgetId(20L);
        request.setSaveAsNotification(true);
        Budget budget = new Budget()
            .cycle(BudgetCycle.MONTHLY)
            .periodStart(LocalDate.of(2026, 6, 1))
            .periodEnd(LocalDate.of(2026, 6, 30))
            .limitAmount(new BigDecimal("1000.00"))
            .alertThreshold(new BigDecimal("0.80"))
            .enabled(true)
            .ledger(ledger);
        budget.setId(20L);
        TransactionRecord expense = new TransactionRecord()
            .ledger(ledger)
            .type(TransactionType.EXPENSE)
            .amount(new BigDecimal("900.00"))
            .transactionDate(LocalDate.of(2026, 6, 15));
        AiAlertResultDTO parsed = new AiAlertResultDTO();
        parsed.setTitle("预算提醒");
        parsed.setContent("预算即将超限。");
        parsed.setLevel(AiAlertResultDTO.AlertLevel.WARNING);
        parsed.setNeedNotification(true);
        NotificationMessage savedNotification = new NotificationMessage();
        savedNotification.setId(99L);

        when(ledgerRepository.findById(10L)).thenReturn(Optional.of(ledger));
        when(budgetRepository.findById(20L)).thenReturn(Optional.of(budget));
        when(transactionRecordRepository.findAllByLedgerAndTransactionDateBetween(ledger, budget.getPeriodStart(), budget.getPeriodEnd()))
            .thenReturn(List.of(expense));
        when(transactionRecordRepository.findAllByLedgerAndTransactionDateBetween(ledger, budget.getPeriodEnd().minusDays(14), budget.getPeriodEnd()))
            .thenReturn(List.of(expense));
        when(alertPromptBuilder.build(any(), any(), any(), any())).thenReturn("alert-prompt");
        when(llmClient.complete("alert-prompt")).thenReturn("{alert-json}");
        when(parser.parseAlert("{alert-json}")).thenReturn(parsed);
        when(guardService.validateAlertResult(parsed)).thenReturn(parsed);
        when(notificationMessageRepository.save(any(NotificationMessage.class))).thenReturn(savedNotification);

        AiAlertResultDTO result = service.generateBudgetAlert(request, currentUser);

        ArgumentCaptor<NotificationMessage> notificationCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationMessageRepository).save(notificationCaptor.capture());
        NotificationMessage notification = notificationCaptor.getValue();
        assertThat(notification.getTitle()).isEqualTo("预算提醒");
        assertThat(notification.getType()).isEqualTo(NotificationType.BUDGET_ALERT);
        assertThat(notification.getLevel()).isEqualTo(NotificationLevel.WARNING);
        assertThat(notification.getUser()).isSameAs(currentUser);
        assertThat(notification.getLedger()).isSameAs(ledger);
        assertThat(notification.getBudget()).isSameAs(budget);
        assertThat(result.getNotificationId()).isEqualTo(99L);
    }
}
