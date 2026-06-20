package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.llm.dto.AiAlertRequestDTO;
import com.everycent.service.BudgetService;
import com.everycent.service.LlmParsingService;
import com.everycent.service.NotificationService;
import com.everycent.service.TransactionRecordService;
import com.everycent.service.dto.BudgetDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FinanceWriteSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of(
        "transaction.create",
        "budget.create",
        "budget.update",
        "notification.mark_read",
        "budget.alert.generate"
    );

    private final TransactionRecordService transactionRecordService;

    private final BudgetService budgetService;

    private final NotificationService notificationService;

    private final LlmParsingService llmParsingService;

    private final SkillCurrentUserResolver currentUserResolver;

    public FinanceWriteSkill(
        TransactionRecordService transactionRecordService,
        BudgetService budgetService,
        NotificationService notificationService,
        LlmParsingService llmParsingService,
        SkillCurrentUserResolver currentUserResolver
    ) {
        this.transactionRecordService = transactionRecordService;
        this.budgetService = budgetService;
        this.notificationService = notificationService;
        this.llmParsingService = llmParsingService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "finance-write-skill";
    }

    @Override
    public String description() {
        return "Create low-risk financial records, budgets, read markers and budget alerts.";
    }

    @Override
    public boolean supports(String actionName) {
        return ACTIONS.contains(actionName);
    }

    @Override
    public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
        User user = currentUserResolver.resolve(context);
        ActionArgumentReader args = new ActionArgumentReader(action);
        return switch (action.getName()) {
            case "transaction.create" -> SkillResult.success(
                action.getName(),
                transactionRecordService.create(user, args.longValue("ledgerId"), transactionDTO(args))
            );
            case "budget.create" -> SkillResult.success(action.getName(), budgetService.create(user, args.longValue("ledgerId"), budgetDTO(args)));
            case "budget.update" -> SkillResult.success(action.getName(), budgetService.update(user, args.longValue("budgetId"), budgetDTO(args)));
            case "notification.mark_read" -> SkillResult.success(
                action.getName(),
                notificationService.markAsRead(user, args.longValue("notificationId"))
            );
            case "budget.alert.generate" -> SkillResult.success(action.getName(), llmParsingService.generateBudgetAlert(alertRequest(args), user));
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "FinanceWriteSkill 不支持该 action");
        };
    }

    private TransactionRecordDTO transactionDTO(ActionArgumentReader args) {
        TransactionRecordDTO dto = new TransactionRecordDTO();
        dto.setLedgerId(args.longValue("ledgerId"));
        dto.setAmount(args.decimalValue("amount"));
        dto.setType(args.transactionType("type"));
        dto.setBehaviorTagId(args.longValue("behaviorTagId"));
        dto.setEmotionTagId(args.longValue("emotionTagId"));
        dto.setTransactionDate(args.dateValue("recordDate"));
        dto.setDescription(args.stringValue("description"));
        dto.setSource(args.recordSource("source"));
        dto.setRawInput(args.stringValue("rawInput"));
        return dto;
    }

    private BudgetDTO budgetDTO(ActionArgumentReader args) {
        BudgetDTO dto = new BudgetDTO();
        dto.setLedgerId(args.longValue("ledgerId"));
        dto.setCycle(args.budgetCycle("cycle"));
        dto.setPeriodStart(args.dateValue("periodStart"));
        dto.setPeriodEnd(args.dateValue("periodEnd"));
        dto.setLimitAmount(args.decimalValue("limitAmount"));
        dto.setAlertThreshold(args.decimalValue("alertThreshold"));
        dto.setEnabled(args.booleanValue("enabled"));
        return dto;
    }

    private AiAlertRequestDTO alertRequest(ActionArgumentReader args) {
        AiAlertRequestDTO request = new AiAlertRequestDTO();
        request.setLedgerId(args.longValue("ledgerId"));
        request.setBudgetId(args.longValue("budgetId"));
        request.setSaveAsNotification(args.booleanValue("saveAsNotification"));
        return request;
    }
}
