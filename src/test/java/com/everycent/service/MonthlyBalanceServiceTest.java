package com.everycent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.everycent.domain.Ledger;
import com.everycent.domain.MonthlyBalance;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.MonthlyBalanceRepository;
import com.everycent.repository.TransactionRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonthlyBalanceServiceTest {

    private MonthlyBalanceRepository monthlyBalanceRepository;

    private TransactionRecordRepository transactionRecordRepository;

    private MonthlyBalanceService service;

    private Ledger ledger;

    @BeforeEach
    void setUp() {
        monthlyBalanceRepository = org.mockito.Mockito.mock(MonthlyBalanceRepository.class);
        transactionRecordRepository = org.mockito.Mockito.mock(TransactionRecordRepository.class);
        service = new MonthlyBalanceService(monthlyBalanceRepository, transactionRecordRepository);

        ledger = new Ledger();
        ledger.setId(10L);
        ledger.setName("Test ledger");
    }

    @Test
    void recalculateShouldCreateMonthlyBalanceFromTransactions() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        when(monthlyBalanceRepository.findOneByLedgerAndYearAndMonth(ledger, 2026, 6)).thenReturn(Optional.empty());
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.INCOME,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("5000.00"));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
                ledger,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
            ))
            .thenReturn(new BigDecimal("1200.50"));
        when(monthlyBalanceRepository.save(any(MonthlyBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyBalance result = service.recalculate(ledger, date);

        assertThat(result.getLedger()).isEqualTo(ledger);
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getMonth()).isEqualTo(6);
        assertThat(result.getTotalIncome()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("1200.50");
        assertThat(result.getBalance()).isEqualByComparingTo("3799.50");
        assertThat(result.getUpdatedDate()).isNotNull();
    }

    @Test
    void recalculateShouldUpdateExistingMonthlyBalance() {
        MonthlyBalance existing = new MonthlyBalance();
        existing.setId(100L);
        existing.setLedger(ledger);
        existing.setYear(2026);
        existing.setMonth(6);
        when(monthlyBalanceRepository.findOneByLedgerAndYearAndMonth(ledger, 2026, 6)).thenReturn(Optional.of(existing));
        when(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(any(), any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(monthlyBalanceRepository.save(any(MonthlyBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyBalance result = service.recalculate(ledger, LocalDate.of(2026, 6, 18));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getBalance()).isEqualByComparingTo("0");
    }
}
