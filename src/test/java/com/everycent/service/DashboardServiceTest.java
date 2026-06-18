package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.BudgetCycle;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.repository.projection.DateAmountProjection;
import com.everycent.repository.projection.TagAmountProjection;
import com.everycent.service.dto.DashboardSummaryDTO;
import com.everycent.service.dto.TagStatDTO;
import com.everycent.service.dto.TrendPointDTO;
import com.everycent.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private TransactionRecordRepository transactionRecordRepository;

    private BudgetRepository budgetRepository;

    private LedgerPermissionService ledgerPermissionService;

    private DashboardService service;

    private User user;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        transactionRecordRepository = org.mockito.Mockito.mock(TransactionRecordRepository.class);
        budgetRepository = org.mockito.Mockito.mock(BudgetRepository.class);
        ledgerPermissionService = org.mockito.Mockito.mock(LedgerPermissionService.class);
        service = new DashboardService(transactionRecordRepository, budgetRepository, ledgerPermissionService);

        user = new User();
        user.setId(1L);
        user.setLogin("admin");

        ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("Test ledger");

        when(ledgerPermissionService.getLedgerOrThrow(10L)).thenReturn(ledger);
    }

    @Test
    void getSummaryShouldReturnMonthlyTotalsAndBudgetStatus() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        Budget budget = budget();

        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(ledger, TransactionType.INCOME, start, end))
            .thenReturn(new BigDecimal("8000.00"));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(ledger, TransactionType.EXPENSE, start, end))
            .thenReturn(new BigDecimal("2000.00"));
        when(transactionRecordRepository.countByLedgerAndTransactionDateBetween(ledger, start, end)).thenReturn(5L);
        when(budgetRepository.findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(ledger, BudgetCycle.MONTHLY, start, end))
            .thenReturn(Optional.of(budget));

        DashboardSummaryDTO result = service.getSummary(user, 10L, "MONTH", LocalDate.of(2026, 6, 18));

        assertThat(result.getTotalIncome()).isEqualByComparingTo("8000.00");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("2000.00");
        assertThat(result.getBalance()).isEqualByComparingTo("6000.00");
        assertThat(result.getTransactionCount()).isEqualTo(5L);
        assertThat(result.getBudgetUsedRatio()).isEqualByComparingTo("0.8000");
        assertThat(result.getBudgetAlertLevel()).isEqualTo("WARNING");
        verify(ledgerPermissionService).checkReadPermission(user, 10L);
    }

    @Test
    void getTrendShouldReturnDatePoints() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(transactionRecordRepository.sumIncomeAndExpenseByDate(ledger, start, end))
            .thenReturn(List.of(dateAmount(LocalDate.of(2026, 6, 1), new BigDecimal("100.00"), new BigDecimal("28.50"))));

        List<TrendPointDTO> result = service.getTrend(user, 10L, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.get(0).getIncome()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).getExpense()).isEqualByComparingTo("28.50");
    }

    @Test
    void getBehaviorTagStatsShouldReturnRatios() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(transactionRecordRepository.sumExpenseByBehaviorTag(ledger, start, end))
            .thenReturn(List.of(tagAmount(1L, "Food", new BigDecimal("80.00"), 2L), tagAmount(2L, "Transport", new BigDecimal("20.00"), 1L)));

        List<TagStatDTO> result = service.getBehaviorTagStats(user, 10L, "MONTH", LocalDate.of(2026, 6, 18));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTagName()).isEqualTo("Food");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("80.00");
        assertThat(result.get(0).getCount()).isEqualTo(2L);
        assertThat(result.get(0).getRatio()).isEqualByComparingTo("0.8000");
        assertThat(result.get(0).getPercentage()).isEqualByComparingTo("0.8000");
    }

    @Test
    void getBehaviorTagStatsShouldAcceptDuplicatedPeriodQueryValue() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(transactionRecordRepository.sumExpenseByBehaviorTag(ledger, start, end)).thenReturn(List.of());

        List<TagStatDTO> result = service.getBehaviorTagStats(user, 10L, "MONTH,MONTH", LocalDate.of(2026, 6, 18));

        assertThat(result).isEmpty();
    }

    @Test
    void getEmotionTagStatsShouldReturnRatios() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        when(transactionRecordRepository.sumExpenseByEmotionTag(ledger, start, end))
            .thenReturn(List.of(tagAmount(1L, "Happy", new BigDecimal("50.00"), 1L), tagAmount(2L, "Stress", new BigDecimal("50.00"), 1L)));

        List<TagStatDTO> result = service.getEmotionTagStats(user, 10L, "MONTH", LocalDate.of(2026, 6, 18));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRatio()).isEqualByComparingTo("0.5000");
    }

    @Test
    void getTrendShouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> service.getTrend(user, 10L, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)))
            .isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void getTrendShouldDefaultToCurrentMonthWhenDateRangeIsBlank() {
        when(transactionRecordRepository.sumIncomeAndExpenseByDate(ledger, LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())))
            .thenReturn(List.of());

        List<TrendPointDTO> result = service.getTrend(user, 10L, null, null);

        assertThat(result).isEmpty();
    }

    private Budget budget() {
        Budget budget = new Budget();
        budget.setId(100L);
        budget.setLedger(ledger);
        budget.setCycle(BudgetCycle.MONTHLY);
        budget.setPeriodStart(LocalDate.of(2026, 6, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 6, 30));
        budget.setLimitAmount(new BigDecimal("2500.00"));
        budget.setAlertThreshold(new BigDecimal("0.80"));
        budget.setEnabled(true);
        return budget;
    }

    private DateAmountProjection dateAmount(LocalDate date, BigDecimal income, BigDecimal expense) {
        return new DateAmountProjection() {
            @Override
            public LocalDate getDate() {
                return date;
            }

            @Override
            public BigDecimal getIncome() {
                return income;
            }

            @Override
            public BigDecimal getExpense() {
                return expense;
            }
        };
    }

    private TagAmountProjection tagAmount(Long tagId, String tagName, BigDecimal amount, Long count) {
        return new TagAmountProjection() {
            @Override
            public Long getTagId() {
                return tagId;
            }

            @Override
            public String getTagName() {
                return tagName;
            }

            @Override
            public BigDecimal getAmount() {
                return amount;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
