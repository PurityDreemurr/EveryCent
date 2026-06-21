package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.repository.UserRepository;
import com.everycent.service.BudgetService;
import com.everycent.service.LedgerService;
import com.everycent.service.LlmParsingService;
import com.everycent.service.NotificationService;
import com.everycent.service.TransactionRecordService;
import com.everycent.service.dto.BudgetDTO;
import com.everycent.service.dto.LedgerCreateDTO;
import com.everycent.service.dto.LedgerDTO;
import com.everycent.service.dto.LedgerUpdateDTO;
import com.everycent.service.dto.NotificationMessageDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LowRiskWriteSkillTest {

    private User user;

    private SkillCurrentUserResolver currentUserResolver;

    private SkillExecutionContext context;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("alice");

        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        currentUserResolver = new SkillCurrentUserResolver(userRepository);
        context = new SkillExecutionContext();
        context.setUserId(1L);
    }

    @Test
    void ledgerWriteSkillShouldCreateAndUpdateLedger() {
        LedgerService ledgerService = org.mockito.Mockito.mock(LedgerService.class);
        LedgerWriteSkill skill = new LedgerWriteSkill(ledgerService, currentUserResolver);
        when(ledgerService.createLedger(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any())).thenReturn(new LedgerDTO());
        when(ledgerService.updateLedger(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new LedgerDTO());

        SkillResult createResult = skill.execute(new AssistantAction("ledger.create", Map.of("name", "日常账本", "description", "日常")), context);
        SkillResult updateResult = skill.execute(new AssistantAction("ledger.update", Map.of("ledgerId", 10L, "name", "新账本", "description", "更新")), context);

        assertThat(createResult.getSuccess()).isTrue();
        assertThat(updateResult.getSuccess()).isTrue();
        verify(ledgerService)
            .createLedger(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.argThat(dto ->
                    dto instanceof LedgerCreateDTO && "日常账本".equals(dto.getName()) && "日常".equals(dto.getDescription())
                )
            );
        verify(ledgerService)
            .updateLedger(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(dto ->
                    dto instanceof LedgerUpdateDTO && "新账本".equals(dto.getName()) && "更新".equals(dto.getDescription())
                )
            );
    }

    @Test
    void financeWriteSkillShouldCreateTransaction() {
        TransactionRecordService transactionService = org.mockito.Mockito.mock(TransactionRecordService.class);
        FinanceWriteSkill skill = financeWriteSkill(transactionService, org.mockito.Mockito.mock(BudgetService.class), org.mockito.Mockito.mock(NotificationService.class), org.mockito.Mockito.mock(LlmParsingService.class));
        when(transactionService.create(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new TransactionRecordDTO());

        SkillResult result = skill.execute(
            new AssistantAction(
                "transaction.create",
                Map.of(
                    "ledgerId",
                    10L,
                    "amount",
                    "28.50",
                    "type",
                    "EXPENSE",
                    "behaviorTagId",
                    2L,
                    "emotionTagId",
                    3L,
                    "recordDate",
                    "2026-06-18",
                    "description",
                    "午饭",
                    "source",
                    "MANUAL",
                    "rawInput",
                    "午饭28.5"
                )
            ),
            context
        );

        assertThat(result.getSuccess()).isTrue();
        verify(transactionService)
            .create(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(dto ->
                    dto instanceof TransactionRecordDTO
                        && dto.getAmount().compareTo(new BigDecimal("28.50")) == 0
                        && dto.getType() == TransactionType.EXPENSE
                        && dto.getBehaviorTagId().equals(2L)
                        && dto.getEmotionTagId().equals(3L)
                        && dto.getTransactionDate().equals(LocalDate.of(2026, 6, 18))
                        && "午饭".equals(dto.getDescription())
                        && dto.getSource() == RecordSource.MANUAL
                        && "午饭28.5".equals(dto.getRawInput())
                )
            );
    }

    @Test
    void financeWriteSkillShouldCreateAndUpdateBudget() {
        BudgetService budgetService = org.mockito.Mockito.mock(BudgetService.class);
        FinanceWriteSkill skill = financeWriteSkill(org.mockito.Mockito.mock(TransactionRecordService.class), budgetService, org.mockito.Mockito.mock(NotificationService.class), org.mockito.Mockito.mock(LlmParsingService.class));
        when(budgetService.setForPeriod(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new BudgetDTO());
        when(budgetService.update(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new BudgetDTO());
        Map<String, Object> args = Map.of(
            "ledgerId",
            10L,
            "budgetId",
            20L,
            "cycle",
            "MONTHLY",
            "periodStart",
            "2026-06-01",
            "periodEnd",
            "2026-06-30",
            "limitAmount",
            "3000.00",
            "alertThreshold",
            "0.80",
            "enabled",
            true
        );

        assertThat(skill.execute(new AssistantAction("budget.create", args), context).getSuccess()).isTrue();
        assertThat(skill.execute(new AssistantAction("budget.update", args), context).getSuccess()).isTrue();

        verify(budgetService)
            .setForPeriod(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(dto -> matchesBudgetDto(dto))
            );
        verify(budgetService)
            .update(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.argThat(dto -> matchesBudgetDto(dto))
            );
    }

    @Test
    void financeWriteSkillShouldMarkNotificationReadAndGenerateBudgetAlert() {
        NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
        LlmParsingService llmParsingService = org.mockito.Mockito.mock(LlmParsingService.class);
        FinanceWriteSkill skill = financeWriteSkill(org.mockito.Mockito.mock(TransactionRecordService.class), org.mockito.Mockito.mock(BudgetService.class), notificationService, llmParsingService);
        when(notificationService.markAsRead(user, 50L)).thenReturn(new NotificationMessageDTO());
        when(llmParsingService.generateBudgetAlert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(user))).thenReturn(new AiAlertResultDTO());

        assertThat(skill.execute(new AssistantAction("notification.mark_read", Map.of("notificationId", 50L)), context).getSuccess()).isTrue();
        assertThat(
            skill
                .execute(new AssistantAction("budget.alert.generate", Map.of("ledgerId", 10L, "budgetId", 20L, "saveAsNotification", true)), context)
                .getSuccess()
        )
            .isTrue();

        verify(notificationService).markAsRead(user, 50L);
        verify(llmParsingService)
            .generateBudgetAlert(
                org.mockito.ArgumentMatchers.argThat(request ->
                    request.getLedgerId().equals(10L)
                        && request.getBudgetId().equals(20L)
                        && Boolean.TRUE.equals(request.getSaveAsNotification())
                ),
                org.mockito.ArgumentMatchers.eq(user)
            );
    }

    @Test
    void routerShouldExecuteLowRiskWriteSkillButStillBlockDeleteAndAccountActions() {
        LedgerService ledgerService = org.mockito.Mockito.mock(LedgerService.class);
        when(ledgerService.createLedger(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any())).thenReturn(new LedgerDTO());
        SkillRouter router = new SkillRouter(
            new SkillRegistry(List.of(new LedgerWriteSkill(ledgerService, currentUserResolver))),
            new ActionPolicyService()
        );

        SkillResult createResult = router.route(new AssistantAction("ledger.create", Map.of("name", "test")), context);
        SkillResult deleteResult = router.route(new AssistantAction("ledger.delete"), context);
        SkillResult accountResult = router.route(new AssistantAction("account.update"), context);

        assertThat(createResult.getSuccess()).isTrue();
        assertThat(deleteResult.getSuccess()).isFalse();
        assertThat(deleteResult.getBlockedByPolicy()).isTrue();
        assertThat(accountResult.getSuccess()).isFalse();
        assertThat(accountResult.getBlockedByPolicy()).isTrue();
        verify(ledgerService).createLedger(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any());
    }

    private FinanceWriteSkill financeWriteSkill(
        TransactionRecordService transactionService,
        BudgetService budgetService,
        NotificationService notificationService,
        LlmParsingService llmParsingService
    ) {
        return new FinanceWriteSkill(transactionService, budgetService, notificationService, llmParsingService, currentUserResolver);
    }

    private boolean matchesBudgetDto(BudgetDTO dto) {
        return dto != null
            && dto.getLedgerId().equals(10L)
            && dto.getCycle() == BudgetCycle.MONTHLY
            && dto.getPeriodStart().equals(LocalDate.of(2026, 6, 1))
            && dto.getPeriodEnd().equals(LocalDate.of(2026, 6, 30))
            && dto.getLimitAmount().compareTo(new BigDecimal("3000.00")) == 0
            && dto.getAlertThreshold().compareTo(new BigDecimal("0.80")) == 0
            && Boolean.TRUE.equals(dto.getEnabled());
    }
}
