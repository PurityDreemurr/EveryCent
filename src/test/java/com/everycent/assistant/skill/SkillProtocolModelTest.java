package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillProtocolModelTest {

    @Test
    void shouldDefaultMutableCollectionsToEmpty() {
        AssistantAction action = new AssistantAction();
        action.setArguments(null);

        AssistantPlan plan = new AssistantPlan();
        plan.setActions(null);

        ReplyStyle replyStyle = new ReplyStyle();
        replyStyle.setTags(null);

        assertThat(action.getArguments()).isEmpty();
        assertThat(plan.getActions()).isEmpty();
        assertThat(replyStyle.getTags()).isEmpty();
    }

    @Test
    void shouldExposeActionHelpers() {
        AssistantAction create = new AssistantAction("transaction.create", Map.of("amount", "28.00"));
        AssistantAction delete = new AssistantAction("transaction.delete");

        assertThat(create.isAllowed()).isTrue();
        assertThat(create.isForbidden()).isFalse();
        assertThat(create.riskLevel()).isEqualTo(RiskLevel.EXPLICIT_REQUEST);
        assertThat(create.getArguments()).containsEntry("amount", "28.00");

        assertThat(delete.isAllowed()).isFalse();
        assertThat(delete.isForbidden()).isTrue();
        assertThat(delete.riskLevel()).isEqualTo(RiskLevel.FORBIDDEN);
    }

    @Test
    void shouldBuildStructuredSkillResults() {
        SkillResult success = SkillResult.success("ledger.list", List.of(Map.of("id", 1L)));
        SkillResult confirmation = SkillResult.needConfirmation("transaction.update", Map.of("transactionId", 1L), "请确认修改");
        SkillResult blocked = SkillResult.blocked("transaction.delete", "AI 助手不能执行删除操作");

        assertThat(success.getSuccess()).isTrue();
        assertThat(success.getActionName()).isEqualTo("ledger.list");
        assertThat(success.getData()).isInstanceOf(List.class);

        assertThat(confirmation.getSuccess()).isFalse();
        assertThat(confirmation.getNeedUserConfirmation()).isTrue();
        assertThat(confirmation.getMessage()).isEqualTo("请确认修改");

        assertThat(blocked.getSuccess()).isFalse();
        assertThat(blocked.getBlockedByPolicy()).isTrue();
        assertThat(blocked.getErrorCode()).isEqualTo("ACTION_FORBIDDEN");
    }

    @Test
    void shouldHoldExecutionContextForFutureRouter() {
        SkillExecutionContext context = new SkillExecutionContext();
        context.setUserId(10L);
        context.setDefaultLedgerId(20L);
        context.setSessionId("session-1");
        context.setOriginalInput("外卖花了28");
        context.setDryRun(true);

        assertThat(context.getUserId()).isEqualTo(10L);
        assertThat(context.getDefaultLedgerId()).isEqualTo(20L);
        assertThat(context.getSessionId()).isEqualTo("session-1");
        assertThat(context.getOriginalInput()).isEqualTo("外卖花了28");
        assertThat(context.getDryRun()).isTrue();
    }
}
