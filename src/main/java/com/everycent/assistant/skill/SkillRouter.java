package com.everycent.assistant.skill;

import org.springframework.stereotype.Component;

@Component
public class SkillRouter {

    private final SkillRegistry skillRegistry;

    private final ActionPolicyService actionPolicyService;

    public SkillRouter(SkillRegistry skillRegistry, ActionPolicyService actionPolicyService) {
        this.skillRegistry = skillRegistry;
        this.actionPolicyService = actionPolicyService;
    }

    public SkillResult route(AssistantAction action, SkillExecutionContext context) {
        try {
            ActionPolicyDecision decision = actionPolicyService.evaluate(action);
            if (decision.isNeedConfirmation()) {
                return SkillResult.needConfirmation(action.getName(), null, decision.getMessage());
            }
            if (!decision.isAllowed()) {
                return blockedResult(action, decision);
            }

            Skill skill = skillRegistry.findSkill(action).orElseThrow(() -> new SkillNotFoundException("没有可执行该 action 的 Skill：" + action.getName()));
            SkillResult result = skill.execute(action, context);
            if (result == null) {
                return SkillResult.failure(action.getName(), "SKILL_EMPTY_RESULT", "Skill 未返回执行结果");
            }
            return result;
        } catch (InvalidActionException e) {
            return SkillResult.failure(actionName(action), "INVALID_ACTION", e.getMessage());
        } catch (ForbiddenActionException e) {
            return SkillResult.blocked(actionName(action), e.getMessage());
        } catch (SkillNotFoundException e) {
            return SkillResult.failure(actionName(action), "SKILL_NOT_FOUND", e.getMessage());
        }
    }

    private SkillResult blockedResult(AssistantAction action, ActionPolicyDecision decision) {
        SkillResult result = SkillResult.blocked(actionName(action), decision.getMessage());
        result.setErrorCode(decision.getErrorCode());
        return result;
    }

    private String actionName(AssistantAction action) {
        return action == null ? null : action.getName();
    }
}
