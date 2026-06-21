package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.repository.UserRepository;
import com.everycent.service.BudgetService;
import com.everycent.service.DashboardService;
import com.everycent.service.ExcelExportService;
import com.everycent.service.LedgerService;
import com.everycent.service.LlmParsingService;
import com.everycent.service.NotificationService;
import com.everycent.service.TagService;
import com.everycent.service.TransactionRecordService;
import com.everycent.service.dto.BehaviorTagDTO;
import com.everycent.service.dto.BudgetDTO;
import com.everycent.service.dto.DashboardSummaryDTO;
import com.everycent.service.dto.EmotionTagDTO;
import com.everycent.service.dto.LedgerDTO;
import com.everycent.service.dto.LedgerMemberDTO;
import com.everycent.service.dto.NotificationPageDTO;
import com.everycent.service.dto.TagStatDTO;
import com.everycent.service.dto.TransactionPageDTO;
import com.everycent.service.dto.TransactionQueryDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import com.everycent.service.dto.TrendPointDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadOnlySkillTest {

    private UserRepository userRepository;

    private SkillCurrentUserResolver currentUserResolver;

    private SkillExecutionContext context;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("alice");

        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        currentUserResolver = new SkillCurrentUserResolver(userRepository);
        context = new SkillExecutionContext();
        context.setUserId(1L);
    }

    @Test
    void ledgerReadSkillShouldDelegateListGetAndMembers() {
        LedgerService service = org.mockito.Mockito.mock(LedgerService.class);
        LedgerReadSkill skill = new LedgerReadSkill(service, currentUserResolver);
        when(service.findLedgersForUser(user)).thenReturn(List.of(new LedgerDTO()));
        when(service.findOne(user, 10L)).thenReturn(new LedgerDTO());
        when(service.findMembers(user, 10L)).thenReturn(List.of(new LedgerMemberDTO()));

        assertThat(skill.execute(new AssistantAction("ledger.list"), context).getData()).isInstanceOf(List.class);
        assertThat(skill.execute(new AssistantAction("ledger.get", Map.of("ledgerId", 10L)), context).getData()).isInstanceOf(LedgerDTO.class);
        assertThat(skill.execute(new AssistantAction("ledger.member.list", Map.of("ledgerId", 10L)), context).getData()).isInstanceOf(List.class);

        verify(service).findLedgersForUser(user);
        verify(service).findOne(user, 10L);
        verify(service).findMembers(user, 10L);
    }

    @Test
    void transactionReadSkillShouldDelegateListGetAndParse() {
        TransactionRecordService transactionService = org.mockito.Mockito.mock(TransactionRecordService.class);
        LlmParsingService llmParsingService = org.mockito.Mockito.mock(LlmParsingService.class);
        TransactionReadSkill skill = new TransactionReadSkill(transactionService, llmParsingService, currentUserResolver);
        when(transactionService.findByLedger(any(), any(), any())).thenReturn(new TransactionPageDTO());
        when(transactionService.findOne(user, 99L)).thenReturn(new TransactionRecordDTO());
        when(llmParsingService.parseTransaction(any(), any())).thenReturn(new TransactionParseResultDTO());

        SkillResult listResult = skill.execute(
            new AssistantAction(
                "transaction.list",
                Map.of(
                    "ledgerId",
                    10L,
                    "startDate",
                    "2026-06-01",
                    "endDate",
                    "2026-06-30",
                    "type",
                    "EXPENSE",
                    "page",
                    1,
                    "size",
                    5
                )
            ),
            context
        );
        assertThat(listResult.getData()).isInstanceOf(TransactionPageDTO.class);
        assertThat(skill.execute(new AssistantAction("transaction.get", Map.of("transactionId", 99L)), context).getData()).isInstanceOf(TransactionRecordDTO.class);
        assertThat(skill.execute(new AssistantAction("transaction.parse", Map.of("ledgerId", 10L, "text", "午饭28")), context).getData())
            .isInstanceOf(TransactionParseResultDTO.class);

        verify(transactionService)
            .findByLedger(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(query ->
                    query instanceof TransactionQueryDTO
                        && query.getType() == TransactionType.EXPENSE
                        && query.getPage() == 1
                        && query.getSize() == 5
                )
            );
        verify(transactionService).findOne(user, 99L);
        verify(llmParsingService).parseTransaction(org.mockito.ArgumentMatchers.argThat(request -> request.getLedgerId().equals(10L) && request.getText().equals("午饭28")), org.mockito.ArgumentMatchers.eq(user));
    }

    @Test
    void budgetReadSkillShouldDelegateListAndStatus() {
        BudgetService service = org.mockito.Mockito.mock(BudgetService.class);
        BudgetReadSkill skill = new BudgetReadSkill(service, currentUserResolver);
        when(service.findByLedger(user, 10L)).thenReturn(List.of(new BudgetDTO()));
        when(service.getStatus(user, 10L, BudgetCycle.MONTHLY, LocalDate.of(2026, 6, 18))).thenReturn(new BudgetDTO());

        assertThat(skill.execute(new AssistantAction("budget.list", Map.of("ledgerId", 10L)), context).getData()).isInstanceOf(List.class);
        assertThat(
            skill
                .execute(new AssistantAction("budget.status", Map.of("ledgerId", 10L, "cycle", "MONTHLY", "date", "2026-06-18")), context)
                .getData()
        )
            .isInstanceOf(BudgetDTO.class);

        verify(service).findByLedger(user, 10L);
        verify(service).getStatus(user, 10L, BudgetCycle.MONTHLY, LocalDate.of(2026, 6, 18));
    }

    @Test
    void dashboardReadSkillShouldDelegateAllDashboardQueries() {
        DashboardService service = org.mockito.Mockito.mock(DashboardService.class);
        DashboardReadSkill skill = new DashboardReadSkill(service, currentUserResolver);
        when(service.getSummary(user, 10L, "MONTH", LocalDate.of(2026, 6, 18))).thenReturn(new DashboardSummaryDTO());
        when(service.getTrend(user, 10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))).thenReturn(List.of(new TrendPointDTO()));
        when(service.getBehaviorTagStats(user, 10L, "MONTH", LocalDate.of(2026, 6, 18))).thenReturn(List.of(new TagStatDTO()));
        when(service.getEmotionTagStats(user, 10L, "MONTH", LocalDate.of(2026, 6, 18))).thenReturn(List.of(new TagStatDTO()));

        assertThat(skill.execute(new AssistantAction("dashboard.summary", Map.of("ledgerId", 10L, "period", "MONTH", "date", "2026-06-18")), context).getData())
            .isInstanceOf(DashboardSummaryDTO.class);
        assertThat(
            skill
                .execute(new AssistantAction("dashboard.trend", Map.of("ledgerId", 10L, "startDate", "2026-06-01", "endDate", "2026-06-30")), context)
                .getData()
        )
            .isInstanceOf(List.class);
        assertThat(skill.execute(new AssistantAction("dashboard.behavior_tags", Map.of("ledgerId", 10L, "period", "MONTH", "date", "2026-06-18")), context).getData())
            .isInstanceOf(List.class);
        assertThat(skill.execute(new AssistantAction("dashboard.emotion_tags", Map.of("ledgerId", 10L, "period", "MONTH", "date", "2026-06-18")), context).getData())
            .isInstanceOf(List.class);

        verify(service).getSummary(user, 10L, "MONTH", LocalDate.of(2026, 6, 18));
        verify(service).getTrend(user, 10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        verify(service).getBehaviorTagStats(user, 10L, "MONTH", LocalDate.of(2026, 6, 18));
        verify(service).getEmotionTagStats(user, 10L, "MONTH", LocalDate.of(2026, 6, 18));
    }

    @Test
    void tagReadSkillShouldDelegateTagListsWithoutCurrentUser() {
        TagService service = org.mockito.Mockito.mock(TagService.class);
        TagReadSkill skill = new TagReadSkill(service);
        when(service.findBehaviorTags()).thenReturn(List.of(new BehaviorTagDTO()));
        when(service.findEmotionTags()).thenReturn(List.of(new EmotionTagDTO()));

        assertThat(skill.execute(new AssistantAction("tag.behavior.list"), context).getData()).isInstanceOf(List.class);
        assertThat(skill.execute(new AssistantAction("tag.emotion.list"), context).getData()).isInstanceOf(List.class);

        verify(service).findBehaviorTags();
        verify(service).findEmotionTags();
    }

    @Test
    void notificationReadSkillShouldDelegateList() {
        NotificationService service = org.mockito.Mockito.mock(NotificationService.class);
        NotificationReadSkill skill = new NotificationReadSkill(service, currentUserResolver);
        when(service.findForUser(user, false, 2, 10)).thenReturn(new NotificationPageDTO());

        SkillResult result = skill.execute(new AssistantAction("notification.list", Map.of("read", false, "page", 2, "size", 10)), context);

        assertThat(result.getData()).isInstanceOf(NotificationPageDTO.class);
        verify(service).findForUser(user, false, 2, 10);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportReadSkillShouldReturnMetadataInsteadOfBinaryBytes() {
        ExcelExportService service = org.mockito.Mockito.mock(ExcelExportService.class);
        ExportReadSkill skill = new ExportReadSkill(service, currentUserResolver);
        when(service.exportTransactions(user, 10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))).thenReturn(new byte[] { 1, 2, 3 });

        SkillResult result = skill.execute(
            new AssistantAction("export.transactions", Map.of("ledgerId", 10L, "startDate", "2026-06-01", "endDate", "2026-06-30")),
            context
        );

        assertThat(result.getData()).isInstanceOf(Map.class);
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data)
            .containsEntry("downloadReady", true)
            .containsEntry("byteLength", 3)
            .containsEntry("downloadUrl", "/api/ledgers/10/transactions/export?startDate=2026-06-01&endDate=2026-06-30")
            .containsEntry("fileName", "everycent-transactions.xlsx");
        assertThat(data).doesNotContainKey("bytes");
        verify(service).exportTransactions(user, 10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    void routerShouldExecuteReadOnlySkillAfterStageTwoGatePasses() {
        LedgerService ledgerService = org.mockito.Mockito.mock(LedgerService.class);
        when(ledgerService.findLedgersForUser(user)).thenReturn(List.of(new LedgerDTO()));
        SkillRouter router = new SkillRouter(
            new SkillRegistry(List.of(new LedgerReadSkill(ledgerService, currentUserResolver))),
            new ActionPolicyService()
        );

        SkillResult result = router.route(new AssistantAction("ledger.list"), context);

        assertThat(result.getSuccess()).isTrue();
        verify(ledgerService).findLedgersForUser(user);
    }

    @Test
    void routerShouldReturnStructuredErrorWhenContextHasNoCurrentUser() {
        LedgerService ledgerService = org.mockito.Mockito.mock(LedgerService.class);
        SkillRouter router = new SkillRouter(
            new SkillRegistry(List.of(new LedgerReadSkill(ledgerService, currentUserResolver))),
            new ActionPolicyService()
        );

        SkillResult result = router.route(new AssistantAction("ledger.list"), new SkillExecutionContext());

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_ACTION");
        assertThat(result.getMessage()).contains("当前用户 ID");
    }
}
