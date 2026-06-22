package com.everycent.service;

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
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final String ENTITY_NAME = "dashboard";

    private final TransactionRecordRepository transactionRecordRepository;

    private final BudgetRepository budgetRepository;

    private final LedgerPermissionService ledgerPermissionService;

    public DashboardService(
        TransactionRecordRepository transactionRecordRepository,
        BudgetRepository budgetRepository,
        LedgerPermissionService ledgerPermissionService
    ) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.budgetRepository = budgetRepository;
        this.ledgerPermissionService = ledgerPermissionService;
    }

    public DashboardSummaryDTO getSummary(User currentUser, Long ledgerId, String period, LocalDate date) {
        Ledger ledger = getReadableLedger(currentUser, ledgerId);
        DateRange range = resolvePeriod(period, date);

        BigDecimal totalIncome = sumAmount(ledger, TransactionType.INCOME, range);
        BigDecimal totalExpense = sumAmount(ledger, TransactionType.EXPENSE, range);

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setTotalIncome(totalIncome);
        dto.setTotalExpense(totalExpense);
        dto.setBalance(totalIncome.subtract(totalExpense));
        dto.setTransactionCount(transactionRecordRepository.countByLedgerAndTransactionDateBetween(ledger, range.start(), range.end()));

        findBudgetForRange(ledger, range)
            .ifPresentOrElse(
                budget -> {
                    BigDecimal budgetUsedRatio = totalExpense.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP);
                    dto.setBudgetUsedRatio(budgetUsedRatio);
                    dto.setBudgetAlertLevel(resolveBudgetAlertLevel(totalExpense, budget.getLimitAmount(), budget.getAlertThreshold()));
                },
                () -> {
                    dto.setBudgetUsedRatio(BigDecimal.ZERO);
                    dto.setBudgetAlertLevel("NONE");
                }
            );

        return dto;
    }

    public List<TrendPointDTO> getTrend(User currentUser, Long ledgerId, LocalDate startDate, LocalDate endDate) {
        Ledger ledger = getReadableLedger(currentUser, ledgerId);
        DateRange range = validateRange(startDate, endDate);

        List<DateAmountProjection> projections = transactionRecordRepository.sumIncomeAndExpenseByDate(ledger, range.start(), range.end());
        List<TrendPointDTO> result = new ArrayList<>();
        for (DateAmountProjection projection : projections) {
            TrendPointDTO dto = new TrendPointDTO();
            dto.setDate(projection.getDate());
            dto.setIncome(defaultZero(projection.getIncome()));
            dto.setExpense(defaultZero(projection.getExpense()));
            result.add(dto);
        }
        return result;
    }

    public List<TagStatDTO> getBehaviorTagStats(User currentUser, Long ledgerId, String period, LocalDate date) {
        Ledger ledger = getReadableLedger(currentUser, ledgerId);
        if (isAllPeriod(period)) {
            return toTagStats(transactionRecordRepository.sumExpenseByBehaviorTag(ledger));
        }
        DateRange range = resolvePeriod(period, date);
        return toTagStats(transactionRecordRepository.sumExpenseByBehaviorTag(ledger, range.start(), range.end()));
    }

    public List<TagStatDTO> getEmotionTagStats(User currentUser, Long ledgerId, String period, LocalDate date) {
        Ledger ledger = getReadableLedger(currentUser, ledgerId);
        if (isAllPeriod(period)) {
            return toTagStats(transactionRecordRepository.sumExpenseByEmotionTag(ledger));
        }
        DateRange range = resolvePeriod(period, date);
        return toTagStats(transactionRecordRepository.sumExpenseByEmotionTag(ledger, range.start(), range.end()));
    }

    private Ledger getReadableLedger(User currentUser, Long ledgerId) {
        ledgerPermissionService.checkReadPermission(currentUser, ledgerId);
        return ledgerPermissionService.getLedgerOrThrow(ledgerId);
    }

    private BigDecimal sumAmount(Ledger ledger, TransactionType type, DateRange range) {
        return defaultZero(transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(ledger, type, range.start(), range.end()));
    }

    private List<TagStatDTO> toTagStats(List<TagAmountProjection> projections) {
        BigDecimal totalAmount = projections.stream().map(TagAmountProjection::getAmount).map(this::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);

        return projections
            .stream()
            .map(projection -> {
                TagStatDTO dto = new TagStatDTO();
                BigDecimal amount = defaultZero(projection.getAmount());
                dto.setTagId(projection.getTagId());
                dto.setTagName(projection.getTagName());
                dto.setAmount(amount);
                dto.setCount(projection.getCount());
                dto.setRatio(totalAmount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : amount.divide(totalAmount, 4, RoundingMode.HALF_UP));
                return dto;
            })
            .toList();
    }

    private java.util.Optional<Budget> findBudgetForRange(Ledger ledger, DateRange range) {
        BudgetCycle cycle = isWholeWeek(range) ? BudgetCycle.WEEKLY : BudgetCycle.MONTHLY;
        return budgetRepository.findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(ledger, cycle, range.start(), range.end());
    }

    private String resolveBudgetAlertLevel(BigDecimal usedAmount, BigDecimal limitAmount, BigDecimal alertThreshold) {
        if (usedAmount.compareTo(limitAmount) > 0) {
            return "DANGER";
        }
        BigDecimal usedRatio = usedAmount.divide(limitAmount, 4, RoundingMode.HALF_UP);
        if (usedRatio.compareTo(alertThreshold) >= 0) {
            return "WARNING";
        }
        return "INFO";
    }

    private DateRange resolvePeriod(String period, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        String normalizedPeriod = normalizePeriod(period);
        return switch (normalizedPeriod) {
            case "WEEK", "WEEKLY" -> new DateRange(
                targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                targetDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            );
            case "MONTH", "MONTHLY" -> new DateRange(targetDate.withDayOfMonth(1), targetDate.withDayOfMonth(targetDate.lengthOfMonth()));
            case "YEAR", "YEARLY" -> new DateRange(targetDate.withDayOfYear(1), targetDate.withDayOfYear(targetDate.lengthOfYear()));
            default -> throw new BadRequestAlertException("Invalid dashboard period", ENTITY_NAME, "invalidperiod");
        };
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "MONTH";
        }
        String firstPeriod = period.split(",")[0].trim();
        return firstPeriod.isBlank() ? "MONTH" : firstPeriod.toUpperCase();
    }

    private boolean isAllPeriod(String period) {
        return "ALL".equals(normalizePeriod(period));
    }

    private DateRange validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return resolvePeriod("MONTH", null);
        }
        if (startDate == null || endDate == null) {
            throw new BadRequestAlertException("Date range is required", ENTITY_NAME, "missingdaterange");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestAlertException("Start date cannot be after end date", ENTITY_NAME, "invaliddaterange");
        }
        return new DateRange(startDate, endDate);
    }

    private boolean isWholeWeek(DateRange range) {
        return range.start().getDayOfWeek() == DayOfWeek.MONDAY && range.end().equals(range.start().plusDays(6));
    }

    private BigDecimal defaultZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}
