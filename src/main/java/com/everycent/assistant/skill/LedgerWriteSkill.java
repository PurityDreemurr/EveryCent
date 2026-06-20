package com.everycent.assistant.skill;

import com.everycent.domain.User;
import com.everycent.service.LedgerService;
import com.everycent.service.dto.LedgerCreateDTO;
import com.everycent.service.dto.LedgerUpdateDTO;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LedgerWriteSkill implements Skill {

    private static final Set<String> ACTIONS = Set.of("ledger.create", "ledger.update");

    private final LedgerService ledgerService;

    private final SkillCurrentUserResolver currentUserResolver;

    public LedgerWriteSkill(LedgerService ledgerService, SkillCurrentUserResolver currentUserResolver) {
        this.ledgerService = ledgerService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public String name() {
        return "ledger-write-skill";
    }

    @Override
    public String description() {
        return "Create and update ledgers through existing LedgerService.";
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
            case "ledger.create" -> SkillResult.success(action.getName(), ledgerService.createLedger(user, createDTO(args)));
            case "ledger.update" -> SkillResult.success(action.getName(), ledgerService.updateLedger(user, args.longValue("ledgerId"), updateDTO(args)));
            default -> SkillResult.failure(action.getName(), "ACTION_NOT_SUPPORTED", "LedgerWriteSkill 不支持该 action");
        };
    }

    private LedgerCreateDTO createDTO(ActionArgumentReader args) {
        LedgerCreateDTO dto = new LedgerCreateDTO();
        dto.setName(args.stringValue("name"));
        dto.setDescription(args.stringValue("description"));
        return dto;
    }

    private LedgerUpdateDTO updateDTO(ActionArgumentReader args) {
        LedgerUpdateDTO dto = new LedgerUpdateDTO();
        dto.setName(args.stringValue("name"));
        dto.setDescription(args.stringValue("description"));
        return dto;
    }
}
