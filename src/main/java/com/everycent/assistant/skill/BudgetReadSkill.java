package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.BudgetService;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class BudgetReadSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("budget.list", "budget.status");

    private final BudgetService budgetService;

    private final SkillCurrentUserResolver currentUserResolver;

    public BudgetReadSkill(BudgetService budgetService, SkillCurrentUserResolver currentUserResolver) {
        this.budgetService = budgetService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "budget-read-skill";
    }

    @Override
    public String description() {
        return "Read budgets and budget status through existing BudgetService.";
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
            case "budget.list" -> SkillResult.success(action.getName(), budgetService.findByLedger(user, args.longValue("ledgerId")));
            case "budget.status" -> SkillResult.success(
                action.getName(),
                budgetService.getStatus(user, args.longValue("ledgerId"), args.budgetCycle("cycle"), args.dateValue("date"))
            );
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "BudgetReadSkill 不支持该 action");
        };
    }
}
