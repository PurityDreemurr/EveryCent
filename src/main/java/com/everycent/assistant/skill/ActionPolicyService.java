package com.everycent.assistant.skill;

import org.springframework.stereotype.Component;

@Component
public class ActionPolicyService {

    private final ActionSchemaValidator schemaValidator;

    public ActionPolicyService() {
        this(new ActionSchemaValidator());
    }

    public ActionPolicyService(ActionSchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    public ActionPolicyDecision evaluate(AssistantAction action) {
        schemaValidator.validate(action);

        String actionName = action.getName();
        RiskLevel riskLevel = AssistantActionCatalog.riskLevel(actionName);
        if (AssistantActionCatalog.isForbidden(actionName)) {
            return ActionPolicyDecision.blocked(
                RiskLevel.FORBIDDEN,
                "ACTION_FORBIDDEN",
                "该操作涉及账号/账户或删除/移除能力，AI 助手不能执行"
            );
        }
        if (!AssistantActionCatalog.isAllowed(actionName)) {
            return ActionPolicyDecision.blocked(RiskLevel.FORBIDDEN, "ACTION_NOT_ALLOWED", "Action 不在 Skill 白名单中");
        }
        if (riskLevel == RiskLevel.REQUIRE_CONFIRMATION && !Boolean.TRUE.equals(action.getRequiresConfirmation())) {
            return ActionPolicyDecision.needConfirmation(riskLevel, "该操作需要用户二次确认");
        }
        return ActionPolicyDecision.allowed(riskLevel);
    }

    public void assertExecutable(AssistantAction action) {
        ActionPolicyDecision decision = evaluate(action);
        if (decision.isAllowed()) {
            return;
        }
        if ("ACTION_FORBIDDEN".equals(decision.getErrorCode())) {
            throw new ForbiddenActionException(decision.getMessage());
        }
        throw new InvalidActionException(decision.getMessage());
    }
}
