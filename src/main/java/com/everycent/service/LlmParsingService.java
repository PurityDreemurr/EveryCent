package com.everycent.service;

import com.everycent.domain.Budget;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.User;
import com.everycent.domain.UserLedgerPermission;
import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.client.LlmClient;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.llm.dto.AiAlertResultDTO;
import com.everycent.llm.dto.BudgetStatusDTO;
import com.everycent.llm.dto.EmotionStatDTO;
import com.everycent.llm.dto.TransactionParseRequestDTO;
import com.everycent.llm.dto.TransactionParseResultDTO;
import com.everycent.llm.parser.LlmJsonResponseParser;
import com.everycent.llm.prompt.AlertPromptBuilder;
import com.everycent.llm.prompt.TransactionPromptBuilder;
import com.everycent.repository.BehaviorTagRepository;
import com.everycent.repository.BudgetRepository;
import com.everycent.repository.EmotionTagRepository;
import com.everycent.repository.LedgerRepository;
import com.everycent.repository.TransactionRecordRepository;
import com.everycent.repository.UserLedgerPermissionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LlmParsingService {

    private final LedgerRepository ledgerRepository;
    private final BehaviorTagRepository behaviorTagRepository;
    private final EmotionTagRepository emotionTagRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final UserLedgerPermissionRepository userLedgerPermissionRepository;
    private final TransactionPromptBuilder transactionPromptBuilder;
    private final AlertPromptBuilder alertPromptBuilder;
    private final LlmClient llmClient;
    private final LlmJsonResponseParser parser;
    private final AiResultGuardService guardService;

    public LlmParsingService(
        LedgerRepository ledgerRepository,
        BehaviorTagRepository behaviorTagRepository,
        EmotionTagRepository emotionTagRepository,
        BudgetRepository budgetRepository,
        TransactionRecordRepository transactionRecordRepository,
        UserLedgerPermissionRepository userLedgerPermissionRepository,
        TransactionPromptBuilder transactionPromptBuilder,
        AlertPromptBuilder alertPromptBuilder,
        LlmClient llmClient,
        LlmJsonResponseParser parser,
        AiResultGuardService guardService
    ) {
        this.ledgerRepository = ledgerRepository;
        this.behaviorTagRepository = behaviorTagRepository;
        this.emotionTagRepository = emotionTagRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.userLedgerPermissionRepository = userLedgerPermissionRepository;
        this.transactionPromptBuilder = transactionPromptBuilder;
        this.alertPromptBuilder = alertPromptBuilder;
        this.llmClient = llmClient;
        this.parser = parser;
        this.guardService = guardService;
    }

    public TransactionParseResultDTO parseTransaction(TransactionParseRequestDTO request, User currentUser) {
        if (request == null || request.getLedgerId() == null) {
            throw new InvalidAiResultException("ledgerId 不能为空");
        }
        Ledger ledger = getLedger(request.getLedgerId());
        ensureCanWrite(currentUser, ledger);

        LocalDate defaultDate = request.getTransactionDate() == null ? LocalDate.now() : request.getTransactionDate();
        String prompt = transactionPromptBuilder.build(
            request.getText(),
            ledger,
            behaviorTagRepository.findAll(),
            emotionTagRepository.findAll(),
            defaultDate
        );
        String llmResponse = llmClient.complete(prompt);
        TransactionParseResultDTO result = parser.parseTransaction(llmResponse);
        result.setRawInput(request.getText());
        return guardService.validateTransactionResult(result, request.getLedgerId(), currentUser);
    }

    public AiAlertResultDTO generateBudgetAlert(AiAlertRequestDTO request, User currentUser) {
        if (request == null || request.getLedgerId() == null || request.getBudgetId() == null) {
            throw new InvalidAiResultException("ledgerId 和 budgetId 不能为空");
        }
        Ledger ledger = getLedger(request.getLedgerId());
        ensureCanRead(currentUser, ledger);
        Budget budget = budgetRepository
            .findById(request.getBudgetId())
            .filter(candidate -> candidate.getLedger() != null && Objects.equals(candidate.getLedger().getId(), ledger.getId()))
            .orElseThrow(() -> new InvalidAiResultException("预算不存在或不属于当前账本"));

        List<TransactionRecord> periodRecords = transactionRecordRepository.findAllByLedgerAndTransactionDateBetween(
            ledger,
            budget.getPeriodStart(),
            budget.getPeriodEnd()
        );
        BigDecimal usedAmount = periodRecords
            .stream()
            .filter(record -> record.getType() == TransactionType.EXPENSE)
            .map(TransactionRecord::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = budget.getLimitAmount().subtract(usedAmount);
        BigDecimal usedRatio = budget.getLimitAmount().compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : usedAmount.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP);
        boolean overBudget = usedAmount.compareTo(budget.getLimitAmount()) > 0;

        BudgetStatusDTO budgetStatus = new BudgetStatusDTO();
        budgetStatus.setLimitAmount(budget.getLimitAmount());
        budgetStatus.setUsedAmount(usedAmount);
        budgetStatus.setRemainingAmount(remainingAmount);
        budgetStatus.setUsedRatio(usedRatio);

        List<TransactionRecord> recentRecords = transactionRecordRepository.findAllByLedgerAndTransactionDateBetween(
            ledger,
            budget.getPeriodEnd().minusDays(14),
            budget.getPeriodEnd()
        );
        List<EmotionStatDTO> emotionStats = buildEmotionStats(periodRecords);
        String prompt = alertPromptBuilder.build(ledger, budgetStatus, recentRecords, emotionStats);
        AiAlertResultDTO result = parser.parseAlert(llmClient.complete(prompt));
        result.setOverBudget(overBudget);
        result.setUsedAmount(usedAmount);
        result.setLimitAmount(budget.getLimitAmount());
        result.setUsedRatio(usedRatio);
        result.setNeedNotification(Boolean.TRUE.equals(result.getNeedNotification()) && usedRatio.compareTo(budget.getAlertThreshold()) >= 0);
        return guardService.validateAlertResult(result);
    }

    private Ledger getLedger(Long ledgerId) {
        return ledgerRepository.findById(ledgerId).orElseThrow(() -> new InvalidAiResultException("账本不存在：" + ledgerId));
    }

    private void ensureCanWrite(User user, Ledger ledger) {
        if (!hasPermission(user, ledger, level -> level == PermissionLevel.OWNER || level == PermissionLevel.READ_WRITE)) {
            throw new NoLedgerPermissionException("当前用户没有账本写权限");
        }
    }

    private void ensureCanRead(User user, Ledger ledger) {
        if (!hasPermission(user, ledger, level -> true)) {
            throw new NoLedgerPermissionException("当前用户没有账本读权限");
        }
    }

    private boolean hasPermission(User user, Ledger ledger, Function<PermissionLevel, Boolean> allowed) {
        if (user == null || ledger == null || user.getId() == null) {
            return false;
        }
        if (ledger.getCreator() != null && Objects.equals(ledger.getCreator().getId(), user.getId())) {
            return true;
        }
        return userLedgerPermissionRepository
            .findOneByUserAndLedgerAndStatus(user, ledger, PermissionStatus.ACTIVE)
            .map(UserLedgerPermission::getPermissionLevel)
            .map(allowed)
            .orElse(false);
    }

    private List<EmotionStatDTO> buildEmotionStats(List<TransactionRecord> records) {
        Map<EmotionTag, Long> counts = records
            .stream()
            .map(TransactionRecord::getEmotionTag)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return counts
            .entrySet()
            .stream()
            .map(entry -> {
                EmotionStatDTO stat = new EmotionStatDTO();
                stat.setEmotionTagCode(entry.getKey().getCode());
                stat.setEmotionTagName(entry.getKey().getName());
                stat.setCount(entry.getValue());
                stat.setRatio(total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(entry.getValue()).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP));
                return stat;
            })
            .toList();
    }
}
