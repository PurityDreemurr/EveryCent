package com.everycent.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.repository.TransactionRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetAlertServiceTest {

    private BudgetRepository budgetRepository;

    private TransactionRecordRepository transactionRecordRepository;

    private NotificationMessageRepository notificationMessageRepository;

    private NotificationService notificationService;

    private BudgetAlertService service;

    private User user;

    private Ledger ledger;

    private Budget budget;

    @BeforeEach
    void setUp() {
        budgetRepository = org.mockito.Mockito.mock(BudgetRepository.class);
        transactionRecordRepository = org.mockito.Mockito.mock(TransactionRecordRepository.class);
        notificationMessageRepository = org.mockito.Mockito.mock(NotificationMessageRepository.class);
        notificationService = org.mockito.Mockito.mock(NotificationService.class);
        service = new BudgetAlertService(
            budgetRepository,
            transactionRecordRepository,
            notificationMessageRepository,
            notificationService
        );

        user = new User();
        user.setId(1L);
        user.setLogin("admin");

        ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("家庭账本");

        budget = new Budget();
        budget.setId(100L);
        budget.setLedger(ledger);
        budget.setCycle(BudgetCycle.MONTHLY);
        budget.setPeriodStart(LocalDate.of(2026, 6, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 6, 30));
        budget.setLimitAmount(new BigDecimal("100.00"));
        budget.setAlertThreshold(new BigDecimal("0.80"));
        budget.setEnabled(true);
    }

    @Test
    void checkBudgetAlertsShouldCreateWarningWhenThresholdReached() {
        when(budgetRepository.findAllByLedgerAndEnabledTrue(ledger)).thenReturn(List.of(budget));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("80.00"));
        when(notificationMessageRepository.existsByUserAndBudgetAndTypeAndLevelAndRead(
                user,
                budget,
                NotificationType.BUDGET_ALERT,
                NotificationLevel.WARNING,
                false
            ))
            .thenReturn(false);

        service.checkBudgetAlerts(user, ledger, LocalDate.of(2026, 6, 18));

        verify(notificationService)
            .create(
                org.mockito.Mockito.eq(user),
                org.mockito.Mockito.eq(ledger),
                org.mockito.Mockito.eq(budget),
                org.mockito.Mockito.eq("预算即将超支"),
                org.mockito.Mockito.contains("80.00"),
                org.mockito.Mockito.eq(NotificationType.BUDGET_ALERT),
                org.mockito.Mockito.eq(NotificationLevel.WARNING)
            );
    }

    @Test
    void checkBudgetAlertsShouldCreateDangerWhenOverBudget() {
        when(budgetRepository.findAllByLedgerAndEnabledTrue(ledger)).thenReturn(List.of(budget));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("120.00"));

        service.checkBudgetAlerts(user, ledger, LocalDate.of(2026, 6, 18));

        verify(notificationService)
            .create(
                org.mockito.Mockito.eq(user),
                org.mockito.Mockito.eq(ledger),
                org.mockito.Mockito.eq(budget),
                org.mockito.Mockito.eq("预算已超支"),
                org.mockito.Mockito.contains("120.00"),
                org.mockito.Mockito.eq(NotificationType.BUDGET_ALERT),
                org.mockito.Mockito.eq(NotificationLevel.DANGER)
            );
    }

    @Test
    void checkBudgetAlertsShouldSkipExistingUnreadAlert() {
        when(budgetRepository.findAllByLedgerAndEnabledTrue(ledger)).thenReturn(List.of(budget));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("80.00"));
        when(notificationMessageRepository.existsByUserAndBudgetAndTypeAndLevelAndRead(
                user,
                budget,
                NotificationType.BUDGET_ALERT,
                NotificationLevel.WARNING,
                false
            ))
            .thenReturn(true);

        service.checkBudgetAlerts(user, ledger, LocalDate.of(2026, 6, 18));

        verify(notificationService, never())
            .create(
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any()
            );
    }
}
