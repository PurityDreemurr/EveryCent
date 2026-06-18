package com.everycent.service;

import com.everycent.domain.Ledger;
import com.everycent.domain.MonthlyBalance;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.MonthlyBalanceRepository;
import com.everycent.repository.TransactionRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MonthlyBalanceService {

    private final MonthlyBalanceRepository monthlyBalanceRepository;

    private final TransactionRecordRepository transactionRecordRepository;

    public MonthlyBalanceService(
        MonthlyBalanceRepository monthlyBalanceRepository,
        TransactionRecordRepository transactionRecordRepository
    ) {
        this.monthlyBalanceRepository = monthlyBalanceRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    public MonthlyBalance recalculate(Ledger ledger, LocalDate date) {
        if (ledger == null || date == null) {
            return null;
        }

        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.withDayOfMonth(date.lengthOfMonth());
        BigDecimal totalIncome = sumAmount(ledger, TransactionType.INCOME, start, end);
        BigDecimal totalExpense = sumAmount(ledger, TransactionType.EXPENSE, start, end);

        MonthlyBalance monthlyBalance = monthlyBalanceRepository
            .findOneByLedgerAndYearAndMonth(ledger, date.getYear(), date.getMonthValue())
            .orElseGet(() -> {
                MonthlyBalance created = new MonthlyBalance();
                created.setLedger(ledger);
                created.setYear(date.getYear());
                created.setMonth(date.getMonthValue());
                return created;
            });

        monthlyBalance.setTotalIncome(totalIncome);
        monthlyBalance.setTotalExpense(totalExpense);
        monthlyBalance.setBalance(totalIncome.subtract(totalExpense));
        monthlyBalance.setUpdatedDate(Instant.now());
        return monthlyBalanceRepository.save(monthlyBalance);
    }

    private BigDecimal sumAmount(Ledger ledger, TransactionType type, LocalDate start, LocalDate end) {
        BigDecimal result = transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(ledger, type, start, end);
        return result == null ? BigDecimal.ZERO : result;
    }
}
