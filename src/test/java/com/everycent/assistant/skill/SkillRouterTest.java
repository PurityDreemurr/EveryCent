package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SkillRouterTest {

    private final SkillExecutionContext context = new SkillExecutionContext();

    @Test
    void shouldExecuteMatchingSkillAfterPolicyAllowsAction() {
        CountingSkill skill = new CountingSkill("ledger.list");
        SkillRouter router = routerWith(skill);

        SkillResult result = router.route(new AssistantAction("ledger.list"), context);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getActionName()).isEqualTo("ledger.list");
        assertThat(skill.executions()).isEqualTo(1);
    }

    @Test
    void shouldReturnNeedConfirmationAndNotExecuteSkill() {
        CountingSkill skill = new CountingSkill("transaction.update");
        SkillRouter router = routerWith(skill);
        AssistantAction action = new AssistantAction("transaction.update", Map.of("transactionId", 10L));

        SkillResult result = router.route(action, context);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getNeedUserConfirmation()).isTrue();
        assertThat(result.getMessage()).contains("二次确认");
        assertThat(skill.executions()).isZero();
    }

    @Test
    void shouldExecuteConfirmationRequiredActionAfterConfirmationFlag() {
        CountingSkill skill = new CountingSkill("transaction.update");
        SkillRouter router = routerWith(skill);
        AssistantAction action = new AssistantAction("transaction.update", Map.of("transactionId", 10L));
        action.setRequiresConfirmation(true);

        SkillResult result = router.route(action, context);

        assertThat(result.getSuccess()).isTrue();
        assertThat(skill.executions()).isEqualTo(1);
    }

    @Test
    void shouldBlockForbiddenActionBeforeSkillLookup() {
        CountingSkill skill = new CountingSkill("transaction.delete");
        SkillRouter router = routerWith(skill);
        AssistantAction action = new AssistantAction("transaction.delete");
        action.setRequiresConfirmation(true);

        SkillResult result = router.route(action, context);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getBlockedByPolicy()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo("ACTION_FORBIDDEN");
        assertThat(skill.executions()).isZero();
    }

    @Test
    void shouldReturnStructuredErrorForMissingArguments() {
        CountingSkill skill = new CountingSkill("budget.create");
        SkillRouter router = routerWith(skill);

        SkillResult result = router.route(new AssistantAction("budget.create", Map.of("ledgerId", 1L)), context);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_ACTION");
        assertThat(result.getMessage()).contains("缺少必要参数");
        assertThat(skill.executions()).isZero();
    }

    @Test
    void shouldReturnStructuredErrorWhenNoSkillSupportsAllowedAction() {
        SkillRouter router = routerWith(new CountingSkill("ledger.get"));

        SkillResult result = router.route(new AssistantAction("ledger.list"), context);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("SKILL_NOT_FOUND");
    }

    private SkillRouter routerWith(Skill... skills) {
        return new SkillRouter(new SkillRegistry(List.of(skills)), new ActionPolicyService());
    }

    private static class CountingSkill implements Skill {

        private final String supportedActionName;

        private final AtomicInteger executions = new AtomicInteger();

        private CountingSkill(String supportedActionName) {
            this.supportedActionName = supportedActionName;
        }

        @Override
        public String name() {
            return supportedActionName + ".skill";
        }

        @Override
        public String description() {
            return "Test skill";
        }

        @Override
        public boolean supports(String actionName) {
            return supportedActionName.equals(actionName);
        }

        @Override
        public SkillResult execute(AssistantAction action, SkillExecutionContext context) {
            executions.incrementAndGet();
            return SkillResult.success(action.getName(), Map.of("executed", true));
        }

        private int executions() {
            return executions.get();
        }
    }
}
