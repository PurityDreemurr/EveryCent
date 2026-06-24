package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AssistantActionCatalogTest {

    @Test
    void shouldAllowOnlyDocumentedPlannerActions() {
        assertThat(AssistantActionCatalog.isAllowed("ledger.list")).isTrue();
        assertThat(AssistantActionCatalog.isAllowed("transaction.create_from_text")).isTrue();
        assertThat(AssistantActionCatalog.isAllowed("transaction.correct_recent")).isTrue();
        assertThat(AssistantActionCatalog.isAllowed("budget.alert.generate")).isTrue();
        assertThat(AssistantActionCatalog.isAllowed("chat.ask_clarification")).isTrue();

        assertThat(AssistantActionCatalog.isAllowed("transaction.delete")).isFalse();
        assertThat(AssistantActionCatalog.isAllowed("account.update")).isFalse();
        assertThat(AssistantActionCatalog.isAllowed("assistant.technical_help")).isFalse();
        assertThat(AssistantActionCatalog.isAllowed("repository.execute")).isFalse();
    }

    @Test
    void shouldTreatDeleteAndRemoveActionsAsForbidden() {
        assertThat(AssistantActionCatalog.FORBIDDEN_DELETE_ACTIONS)
            .containsExactlyInAnyOrder(
                "ledger.delete",
                "ledger.member.remove",
                "transaction.delete",
                "budget.delete",
                "notification.delete"
            );

        assertThat(AssistantActionCatalog.FORBIDDEN_DELETE_ACTIONS).allMatch(AssistantActionCatalog::isForbidden);
        assertThat(AssistantActionCatalog.FORBIDDEN_DELETE_ACTIONS).noneMatch(AssistantActionCatalog::isAllowed);
    }

    @Test
    void shouldTreatAccountActionsAsForbidden() {
        assertThat(AssistantActionCatalog.FORBIDDEN_ACCOUNT_ACTIONS)
            .contains(
                "account.get",
                "account.update",
                "account.change_password",
                "account.register",
                "account.reset_password",
                "account.delete",
                "authenticate.login"
            );

        assertThat(AssistantActionCatalog.FORBIDDEN_ACCOUNT_ACTIONS).allMatch(AssistantActionCatalog::isForbidden);
        assertThat(AssistantActionCatalog.FORBIDDEN_ACCOUNT_ACTIONS).noneMatch(AssistantActionCatalog::isAllowed);
    }

    @Test
    void shouldTreatTechnicalHelpAsForbidden() {
        assertThat(AssistantActionCatalog.FORBIDDEN_ASSISTANT_ACTIONS).containsExactly("assistant.technical_help");
        assertThat(AssistantActionCatalog.isForbidden("assistant.technical_help")).isTrue();
        assertThat(AssistantActionCatalog.isAllowed("assistant.technical_help")).isFalse();
    }

    @Test
    void shouldClassifyRiskLevels() {
        assertThat(AssistantActionCatalog.riskLevel("dashboard.summary")).isEqualTo(RiskLevel.AUTO_EXECUTE);
        assertThat(AssistantActionCatalog.riskLevel("budget.create")).isEqualTo(RiskLevel.EXPLICIT_REQUEST);
        assertThat(AssistantActionCatalog.riskLevel("transaction.correct_recent")).isEqualTo(RiskLevel.EXPLICIT_REQUEST);
        assertThat(AssistantActionCatalog.riskLevel("transaction.update")).isEqualTo(RiskLevel.REQUIRE_CONFIRMATION);
        assertThat(AssistantActionCatalog.riskLevel("ledger.delete")).isEqualTo(RiskLevel.FORBIDDEN);
        assertThat(AssistantActionCatalog.riskLevel("assistant.technical_help")).isEqualTo(RiskLevel.FORBIDDEN);
        assertThat(AssistantActionCatalog.riskLevel("java.lang.Runtime.exec")).isEqualTo(RiskLevel.FORBIDDEN);
    }

    @Test
    void shouldDetectForbiddenAndIllegalActionsInPlan() {
        AssistantPlan allowedPlan = new AssistantPlan();
        allowedPlan.setActions(List.of(new AssistantAction("ledger.list"), new AssistantAction("budget.status")));

        AssistantPlan forbiddenPlan = new AssistantPlan();
        forbiddenPlan.setActions(List.of(new AssistantAction("transaction.list"), new AssistantAction("transaction.delete")));

        AssistantPlan illegalPlan = new AssistantPlan();
        illegalPlan.setActions(List.of(new AssistantAction("ledger.list"), new AssistantAction("sql.execute")));

        assertThat(allowedPlan.hasOnlyAllowedActions()).isTrue();
        assertThat(allowedPlan.hasForbiddenAction()).isFalse();

        assertThat(forbiddenPlan.hasOnlyAllowedActions()).isFalse();
        assertThat(forbiddenPlan.hasForbiddenAction()).isTrue();

        assertThat(illegalPlan.hasOnlyAllowedActions()).isFalse();
        assertThat(illegalPlan.hasForbiddenAction()).isFalse();
    }
}
