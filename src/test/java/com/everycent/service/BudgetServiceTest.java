package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.service.dto.BudgetDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetServiceTest {

    private BudgetRepository budgetRepository;

    private TransactionRecordRepository transactionRecordRepository;

    private NotificationMessageRepository notificationMessageRepository;

    private LedgerPermissionService ledgerPermissionService;

    private BudgetService service;

    private User user;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        budgetRepository = org.mockito.Mockito.mock(BudgetRepository.class);
        transactionRecordRepository = org.mockito.Mockito.mock(TransactionRecordRepository.class);
        notificationMessageRepository = org.mockito.Mockito.mock(NotificationMessageRepository.class);
        ledgerPermissionService = org.mockito.Mockito.mock(LedgerPermissionService.class);
        service = new BudgetService(budgetRepository, transactionRecordRepository, notificationMessageRepository, ledgerPermissionService);

        user = new User();
        user.setId(1L);
        user.setLogin("admin");

        ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("Test ledger");
    }

    @Test
    void createShouldSaveBudgetAndCalculateUsage() {
        BudgetDTO requestDTO = budgetDTO();

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(budgetRepository.findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(
                ledger,
                BudgetCycle.MONTHLY,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("80.00"));

        BudgetDTO result = service.create(user, 10L, requestDTO);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getBudgetAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getUsedAmount()).isEqualByComparingTo("80.00");
        assertThat(result.getRemainingAmount()).isEqualByComparingTo("20.00");
        assertThat(result.getUsedRatio()).isEqualByComparingTo("0.8000");
        assertThat(result.getUsageRate()).isEqualByComparingTo("0.8000");
        assertThat(result.getOverBudget()).isFalse();
        assertThat(result.getStatus()).isEqualTo("WARNING");
        verify(ledgerPermissionService).checkWritePermission(user, 10L);
    }

    @Test
    void createShouldRejectDuplicateBudget() {
        BudgetDTO requestDTO = budgetDTO();

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(budgetRepository.findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(
                ledger,
                BudgetCycle.MONTHLY,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(Optional.of(budget(100L)));

        assertThatThrownBy(() -> service.create(user, 10L, requestDTO)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void setForPeriodShouldUpdateExistingBudgetInsteadOfRejectingDuplicate() {
        Budget existing = budget(100L);
        BudgetDTO requestDTO = budgetDTO();
        requestDTO.setLimitAmount(new BigDecimal("3000.00"));
        requestDTO.setAlertThreshold(new BigDecimal("0.80"));

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(budgetRepository.findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(
                ledger,
                BudgetCycle.MONTHLY,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("0.00"));

        BudgetDTO result = service.setForPeriod(user, 10L, requestDTO);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getLimitAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.getAlertThreshold()).isEqualByComparingTo("0.80");
        verify(ledgerPermissionService).checkWritePermission(user, 10L);
        verify(budgetRepository).save(existing);
    }

    @Test
    void createShouldRejectInvalidDateRange() {
        BudgetDTO requestDTO = budgetDTO();
        requestDTO.setPeriodStart(LocalDate.of(2026, 6, 30));
        requestDTO.setPeriodEnd(LocalDate.of(2026, 6, 1));

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);

        assertThatThrownBy(() -> service.create(user, 10L, requestDTO)).isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void findByLedgerShouldCheckReadPermissionAndReturnBudgets() {
        Budget budget = budget(100L);

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(budgetRepository.findAllByLedgerOrderByPeriodStartDesc(ledger)).thenReturn(List.of(budget));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("120.00"));

        List<BudgetDTO> result = service.findByLedger(user, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("DANGER");
        assertThat(result.get(0).getOverBudget()).isTrue();
        verify(ledgerPermissionService).checkReadPermission(user, 10L);
    }

    @Test
    void deleteShouldRemoveBudgetNotificationsBeforeDeletingBudget() {
        Budget budget = budget(100L);
        when(budgetRepository.findById(100L)).thenReturn(Optional.of(budget));

        service.delete(user, 100L);

        verify(ledgerPermissionService).checkWritePermission(user, 10L);
        verify(notificationMessageRepository).deleteAllByBudget(budget);
        verify(budgetRepository).delete(budget);
    }

    @Test
    void getStatusShouldReturnBudgetForTargetDate() {
        Budget budget = budget(100L);
        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
        when(budgetRepository.findAllByLedgerAndEnabledTrue(ledger)).thenReturn(List.of(budget));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("10.00"));

        BudgetDTO result = service.getStatus(user, 10L, BudgetCycle.MONTHLY, LocalDate.of(2026, 6, 18));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("INFO");
        verify(ledgerPermissionService).checkReadPermission(user, 10L);
    }

    private BudgetDTO budgetDTO() {
        BudgetDTO dto = new BudgetDTO();
        dto.setCycle(BudgetCycle.MONTHLY);
        dto.setPeriodStart(LocalDate.of(2026, 6, 1));
        dto.setPeriodEnd(LocalDate.of(2026, 6, 30));
        dto.setLimitAmount(new BigDecimal("100.00"));
        dto.setAlertThreshold(new BigDecimal("0.80"));
        dto.setEnabled(true);
        return dto;
    }

    private Budget budget(Long id) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setLedger(ledger);
        budget.setCycle(BudgetCycle.MONTHLY);
        budget.setPeriodStart(LocalDate.of(2026, 6, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 6, 30));
        budget.setLimitAmount(new BigDecimal("100.00"));
        budget.setAlertThreshold(new BigDecimal("0.80"));
        budget.setEnabled(true);
        return budget;
    }
}
