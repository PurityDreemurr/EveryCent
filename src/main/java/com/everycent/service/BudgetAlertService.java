package com.everycent.service;

import com.everycent.domain.Budget;
import com.everycent.domain.Ledger;
import com.everycent.domain.User;
import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.NotificationMessageRepository;
import com.everycent.repository.TransactionRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetAlertService {

    private static final Logger LOG = LoggerFactory.getLogger(BudgetAlertService.class);

    private final BudgetRepository budgetRepository;

    private final TransactionRecordRepository transactionRecordRepository;

    private final NotificationMessageRepository notificationMessageRepository;

    private final NotificationService notificationService;

    public BudgetAlertService(
        BudgetRepository budgetRepository,
        TransactionRecordRepository transactionRecordRepository,
        NotificationMessageRepository notificationMessageRepository,
        NotificationService notificationService
    ) {
        this.budgetRepository = budgetRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.notificationMessageRepository = notificationMessageRepository;
        this.notificationService = notificationService;
    }

    public void checkBudgetAlerts(User user, Ledger ledger, LocalDate transactionDate) {
        if (user == null || ledger == null || transactionDate == null) {
            LOG.debug("Skip budget alert check because user, ledger or transactionDate is null");
            return;
        }

        var budgets = budgetRepository.findAllByLedgerAndEnabledTrue(ledger);
        LOG.debug("Checking {} active budgets for ledger {} and date {}", budgets.size(), ledger.getId(), transactionDate);

        budgets
            .stream()
            .filter(budget -> coversDate(budget, transactionDate))
            .forEach(budget -> createAlertIfNeeded(user, ledger, budget));
    }

    private void createAlertIfNeeded(User user, Ledger ledger, Budget budget) {
        BigDecimal usedAmount = transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
            ledger,
            TransactionType.EXPENSE,
            budget.getPeriodStart(),
            budget.getPeriodEnd()
        );
        usedAmount = usedAmount == null ? BigDecimal.ZERO : usedAmount;
        if (budget.getLimitAmount() == null || budget.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            LOG.debug("Skip budget alert for budget {} because limitAmount is invalid", budget.getId());
            return;
        }

        BigDecimal usedRatio = usedAmount.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP);
        NotificationLevel level = resolveLevel(usedAmount, budget.getLimitAmount(), usedRatio, budget.getAlertThreshold());
        LOG.debug(
            "Budget {} usedAmount={}, limitAmount={}, usedRatio={}, resolvedLevel={}",
            budget.getId(),
            usedAmount,
            budget.getLimitAmount(),
            usedRatio,
            level
        );
        if (level == null) {
            return;
        }
        if (hasUnreadAlert(user, budget, level)) {
            LOG.debug("Skip budget alert for budget {} because unread {} alert already exists", budget.getId(), level);
            return;
        }

        notificationService.create(
            user,
            ledger,
            budget,
            title(level),
            content(level, usedAmount, budget.getLimitAmount(), usedRatio),
            NotificationType.BUDGET_ALERT,
            level
        );
        LOG.debug("Created {} budget alert for budget {}", level, budget.getId());
    }

    private boolean coversDate(Budget budget, LocalDate date) {
        return !date.isBefore(budget.getPeriodStart()) && !date.isAfter(budget.getPeriodEnd());
    }

    private NotificationLevel resolveLevel(BigDecimal usedAmount, BigDecimal limitAmount, BigDecimal usedRatio, BigDecimal alertThreshold) {
        if (usedAmount.compareTo(limitAmount) > 0) {
            return NotificationLevel.DANGER;
        }
        BigDecimal threshold = alertThreshold == null ? BigDecimal.valueOf(0.80) : alertThreshold;
        if (usedRatio.compareTo(threshold) >= 0) {
            return NotificationLevel.WARNING;
        }
        return null;
    }

    private boolean hasUnreadAlert(User user, Budget budget, NotificationLevel level) {
        return notificationMessageRepository.existsByUserAndBudgetAndTypeAndLevelAndRead(
            user,
            budget,
            NotificationType.BUDGET_ALERT,
            level,
            false
        );
    }

    private String title(NotificationLevel level) {
        return level == NotificationLevel.DANGER ? "预算已超支" : "预算即将超支";
    }

    private String content(NotificationLevel level, BigDecimal usedAmount, BigDecimal limitAmount, BigDecimal usedRatio) {
        BigDecimal percentage = usedRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        if (level == NotificationLevel.DANGER) {
            return "当前周期预算已超支，已使用 " + usedAmount + " / " + limitAmount + "，使用率 " + percentage + "%。";
        }
        return "当前周期预算接近上限，已使用 " + usedAmount + " / " + limitAmount + "，使用率 " + percentage + "%。";
    }
}
