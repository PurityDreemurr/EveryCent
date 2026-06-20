package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.LedgerService;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LedgerReadSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("ledger.list", "ledger.get", "ledger.member.list");

    private final LedgerService ledgerService;

    private final SkillCurrentUserResolver currentUserResolver;

    public LedgerReadSkill(LedgerService ledgerService, SkillCurrentUserResolver currentUserResolver) {
        this.ledgerService = ledgerService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "ledger-read-skill";
    }

    @Override
    public String description() {
        return "Read ledgers and ledger members through existing LedgerService.";
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
            case "ledger.list" -> SkillResult.success(action.getName(), ledgerService.findLedgersForUser(user));
            case "ledger.get" -> SkillResult.success(action.getName(), ledgerService.findOne(user, args.longValue("ledgerId")));
            case "ledger.member.list" -> SkillResult.success(action.getName(), ledgerService.findMembers(user, args.longValue("ledgerId")));
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "LedgerReadSkill 不支持该 action");
        };
    }
}
