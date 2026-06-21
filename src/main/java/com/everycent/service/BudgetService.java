package com.everycent.service;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetService {

    private static final String ENTITY_NAME = "budget";

    private final BudgetRepository budgetRepository;

    private final TransactionRecordRepository transactionRecordRepository;

    private final NotificationMessageRepository notificationMessageRepository;

    private final LedgerPermissionService ledgerPermissionService;

    public BudgetService(
        BudgetRepository budgetRepository,
        TransactionRecordRepository transactionRecordRepository,
        NotificationMessageRepository notificationMessageRepository,
        LedgerPermissionService ledgerPermissionService
    ) {
        this.budgetRepository = budgetRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.notificationMessageRepository = notificationMessageRepository;
        this.ledgerPermissionService = ledgerPermissionService;
    }

    @Transactional(readOnly = true)
    public List<BudgetDTO> findByLedger(User currentUser, Long ledgerId) {
        ledgerPermissionService.checkReadPermission(currentUser, ledgerId);
        Ledger ledger = ledgerPermissionService.getLedgerOrThrow(ledgerId);
        return budgetRepository.findAllByLedgerOrderByPeriodStartDesc(ledger).stream().map(this::toDTO).toList();
    }

    public BudgetDTO create(User currentUser, Long ledgerId, BudgetDTO budgetDTO) {
        ledgerPermissionService.checkWritePermission(currentUser, ledgerId);
        Ledger ledger = ledgerPermissionService.getLedgerOrThrow(ledgerId);
        validateBudgetDTO(budgetDTO);
        rejectDuplicateBudget(ledger, budgetDTO, null);

        Budget budget = new Budget();
        budget.setLedger(ledger);
        applyEditableFields(budget, budgetDTO);

        return toDTO(budgetRepository.save(budget));
    }

    public BudgetDTO setForPeriod(User currentUser, Long ledgerId, BudgetDTO budgetDTO) {
        ledgerPermissionService.checkWritePermission(currentUser, ledgerId);
        Ledger ledger = ledgerPermissionService.getLedgerOrThrow(ledgerId);
        validateBudgetDTO(budgetDTO);

        Budget budget = budgetRepository
            .findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(
                ledger,
                budgetDTO.getCycle(),
                budgetDTO.getPeriodStart(),
                budgetDTO.getPeriodEnd()
            )
            .orElseGet(() -> {
                Budget created = new Budget();
                created.setLedger(ledger);
                return created;
            });

        applyEditableFields(budget, budgetDTO);
        return toDTO(budgetRepository.save(budget));
    }

    public BudgetDTO update(User currentUser, Long budgetId, BudgetDTO budgetDTO) {
        Budget budget = getBudgetOrThrow(budgetId);
        ledgerPermissionService.checkWritePermission(currentUser, budget.getLedger().getId());
        validateBudgetDTO(budgetDTO);
        rejectDuplicateBudget(budget.getLedger(), budgetDTO, budgetId);

        applyEditableFields(budget, budgetDTO);

        return toDTO(budgetRepository.save(budget));
    }

    public void delete(User currentUser, Long budgetId) {
        Budget budget = getBudgetOrThrow(budgetId);
        ledgerPermissionService.checkWritePermission(currentUser, budget.getLedger().getId());
        notificationMessageRepository.deleteAllByBudget(budget);
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public BudgetDTO getStatus(User currentUser, Long ledgerId, BudgetCycle cycle, LocalDate date) {
        ledgerPermissionService.checkReadPermission(currentUser, ledgerId);
        Ledger ledger = ledgerPermissionService.getLedgerOrThrow(ledgerId);
        LocalDate targetDate = date == null ? LocalDate.now() : date;

        return budgetRepository
            .findAllByLedgerAndEnabledTrue(ledger)
            .stream()
            .filter(budget -> budget.getCycle() == cycle)
            .filter(budget -> !targetDate.isBefore(budget.getPeriodStart()) && !targetDate.isAfter(budget.getPeriodEnd()))
            .findFirst()
            .map(this::toDTO)
            .orElseThrow(() -> new BadRequestAlertException("Budget not found", ENTITY_NAME, "budgetnotfound"));
    }

    private Budget getBudgetOrThrow(Long budgetId) {
        return budgetRepository
            .findById(budgetId)
            .orElseThrow(() -> new BadRequestAlertException("Budget not found", ENTITY_NAME, "budgetnotfound"));
    }

    private void applyEditableFields(Budget budget, BudgetDTO budgetDTO) {
        budget.setCycle(budgetDTO.getCycle());
        budget.setPeriodStart(budgetDTO.getPeriodStart());
        budget.setPeriodEnd(budgetDTO.getPeriodEnd());
        budget.setLimitAmount(budgetDTO.getLimitAmount());
        budget.setAlertThreshold(budgetDTO.getAlertThreshold() == null ? BigDecimal.valueOf(0.80) : budgetDTO.getAlertThreshold());
        budget.setEnabled(budgetDTO.getEnabled() == null || budgetDTO.getEnabled());
    }

    private void validateBudgetDTO(BudgetDTO budgetDTO) {
        if (budgetDTO.getPeriodStart() != null && budgetDTO.getPeriodEnd() != null && budgetDTO.getPeriodStart().isAfter(budgetDTO.getPeriodEnd())) {
            throw new BadRequestAlertException("Budget start date cannot be after end date", ENTITY_NAME, "invaliddaterange");
        }
    }

    private void rejectDuplicateBudget(Ledger ledger, BudgetDTO budgetDTO, Long currentBudgetId) {
        budgetRepository
            .findOneByLedgerAndCycleAndPeriodStartAndPeriodEnd(
                ledger,
                budgetDTO.getCycle(),
                budgetDTO.getPeriodStart(),
                budgetDTO.getPeriodEnd()
            )
            .filter(existing -> !Objects.equals(existing.getId(), currentBudgetId))
            .ifPresent(existing -> {
                throw new BadRequestAlertException("Budget already exists", ENTITY_NAME, "budgetexists");
            });
    }

    private BudgetDTO toDTO(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        BigDecimal usedAmount = calculateUsedAmount(budget);
        BigDecimal remainingAmount = budget.getLimitAmount().subtract(usedAmount);
        BigDecimal usedRatio = usedAmount.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP);

        dto.setId(budget.getId());
        dto.setLedgerId(budget.getLedger().getId());
        dto.setCycle(budget.getCycle());
        dto.setPeriodStart(budget.getPeriodStart());
        dto.setPeriodEnd(budget.getPeriodEnd());
        dto.setLimitAmount(budget.getLimitAmount());
        dto.setAlertThreshold(budget.getAlertThreshold());
        dto.setEnabled(budget.getEnabled());
        dto.setUsedAmount(usedAmount);
        dto.setRemainingAmount(remainingAmount);
        dto.setUsedRatio(usedRatio);
        dto.setStatus(resolveStatus(usedAmount, budget.getLimitAmount(), budget.getAlertThreshold()));
        dto.setOverBudget(usedAmount.compareTo(budget.getLimitAmount()) > 0);
        return dto;
    }

    private BigDecimal calculateUsedAmount(Budget budget) {
        BigDecimal usedAmount = transactionRecordRepository.sumAmountByLedgerAndTypeAndDateBetween(
            budget.getLedger(),
            TransactionType.EXPENSE,
            budget.getPeriodStart(),
            budget.getPeriodEnd()
        );
        return usedAmount == null ? BigDecimal.ZERO : usedAmount;
    }

    private String resolveStatus(BigDecimal usedAmount, BigDecimal limitAmount, BigDecimal alertThreshold) {
        if (usedAmount.compareTo(limitAmount) > 0) {
            return "DANGER";
        }
        BigDecimal usedRatio = usedAmount.divide(limitAmount, 4, RoundingMode.HALF_UP);
        if (usedRatio.compareTo(alertThreshold) >= 0) {
            return "WARNING";
        }
        return "INFO";
    }
}
