package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionPolicyServiceTest {

    private final ActionPolicyService policyService = new ActionPolicyService();

    @Test
    void shouldAllowAutoAndExplicitRequestActions() {
        ActionPolicyDecision readDecision = policyService.evaluate(new AssistantAction("ledger.list"));
        ActionPolicyDecision writeDecision = policyService.evaluate(
            new AssistantAction(
                "budget.create",
                Map.of("ledgerId", 1L, "cycle", "MONTHLY", "periodStart", "2026-06-01", "periodEnd", "2026-06-30", "limitAmount", "3000.00")
            )
        );

        assertThat(readDecision.isAllowed()).isTrue();
        assertThat(readDecision.getRiskLevel()).isEqualTo(RiskLevel.AUTO_EXECUTE);
        assertThat(writeDecision.isAllowed()).isTrue();
        assertThat(writeDecision.getRiskLevel()).isEqualTo(RiskLevel.EXPLICIT_REQUEST);
    }

    @Test
    void shouldReturnNeedConfirmationForRiskyNonForbiddenAction() {
        AssistantAction action = new AssistantAction("transaction.update", Map.of("transactionId", 10L));

        ActionPolicyDecision decision = policyService.evaluate(action);

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.isNeedConfirmation()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.REQUIRE_CONFIRMATION);
    }

    @Test
    void shouldAllowRiskyNonForbiddenActionAfterConfirmationFlag() {
        AssistantAction action = new AssistantAction("transaction.update", Map.of("transactionId", 10L));
        action.setRequiresConfirmation(true);

        ActionPolicyDecision decision = policyService.evaluate(action);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.REQUIRE_CONFIRMATION);
    }

    @Test
    void shouldBlockDeleteActionEvenWhenConfirmationFlagIsTrue() {
        AssistantAction action = new AssistantAction("transaction.delete");
        action.setRequiresConfirmation(true);

        ActionPolicyDecision decision = policyService.evaluate(action);

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.isNeedConfirmation()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("ACTION_FORBIDDEN");
    }

    @Test
    void shouldBlockAccountAction() {
        ActionPolicyDecision decision = policyService.evaluate(new AssistantAction("account.update"));

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("ACTION_FORBIDDEN");
    }

    @Test
    void shouldRejectActionOutsideWhitelist() {
        ActionPolicyDecision decision = policyService.evaluate(new AssistantAction("ledger.archive"));

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("ACTION_NOT_ALLOWED");
    }

    @Test
    void shouldThrowForbiddenExceptionFromAssertExecutable() {
        assertThatThrownBy(() -> policyService.assertExecutable(new AssistantAction("budget.delete")))
            .isInstanceOf(ForbiddenActionException.class)
            .hasMessageContaining("AI 助手不能执行");
    }
}
