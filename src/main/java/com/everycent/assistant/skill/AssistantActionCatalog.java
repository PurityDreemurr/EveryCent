package com.everycent.assistant.skill;

import java.util.Set;

public final class AssistantActionCatalog {

    public static final Set<String> AUTO_EXECUTE_ACTIONS = Set.of(
        "ledger.list",
        "ledger.get",
        "ledger.member.list",
        "transaction.list",
        "transaction.get",
        "transaction.parse",
        "budget.list",
        "budget.status",
        "dashboard.summary",
        "dashboard.trend",
        "dashboard.behavior_tags",
        "dashboard.emotion_tags",
        "tag.behavior.list",
        "tag.emotion.list",
        "notification.list",
        "export.transactions",
        "chat.respond",
        "chat.ask_clarification"
    );

    public static final Set<String> EXPLICIT_REQUEST_ACTIONS = Set.of(
        "ledger.create",
        "ledger.update",
        "transaction.create",
        "transaction.create_from_text",
        "budget.create",
        "budget.update",
        "notification.mark_read",
        "budget.alert.generate"
    );

    public static final Set<String> REQUIRE_CONFIRMATION_ACTIONS = Set.of(
        "ledger.member.add",
        "ledger.member.update",
        "transaction.update"
    );

    public static final Set<String> FORBIDDEN_DELETE_ACTIONS = Set.of(
        "ledger.delete",
        "ledger.member.remove",
        "transaction.delete",
        "budget.delete",
        "notification.delete"
    );

    public static final Set<String> FORBIDDEN_ACCOUNT_ACTIONS = Set.of(
        "account.get",
        "account.update",
        "account.change_password",
        "account.register",
        "account.activate",
        "account.reset_password",
        "account.deactivate",
        "account.delete",
        "authenticate.login",
        "authenticate.logout"
    );

    public static final Set<String> FORBIDDEN_ASSISTANT_ACTIONS = Set.of("assistant.technical_help");

    public static final Set<String> ALLOWED_ACTIONS = union(
        AUTO_EXECUTE_ACTIONS,
        EXPLICIT_REQUEST_ACTIONS,
        REQUIRE_CONFIRMATION_ACTIONS
    );

    public static final Set<String> FORBIDDEN_ACTIONS = union(
        FORBIDDEN_DELETE_ACTIONS,
        FORBIDDEN_ACCOUNT_ACTIONS,
        FORBIDDEN_ASSISTANT_ACTIONS
    );

    private AssistantActionCatalog() {}

    public static boolean isAllowed(String actionName) {
        return ALLOWED_ACTIONS.contains(actionName);
    }

    public static boolean isForbidden(String actionName) {
        return FORBIDDEN_ACTIONS.contains(actionName);
    }

    public static RiskLevel riskLevel(String actionName) {
        if (AUTO_EXECUTE_ACTIONS.contains(actionName)) {
            return RiskLevel.AUTO_EXECUTE;
        }
        if (EXPLICIT_REQUEST_ACTIONS.contains(actionName)) {
            return RiskLevel.EXPLICIT_REQUEST;
        }
        if (REQUIRE_CONFIRMATION_ACTIONS.contains(actionName)) {
            return RiskLevel.REQUIRE_CONFIRMATION;
        }
        return RiskLevel.FORBIDDEN;
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        Set<String> result = new java.util.LinkedHashSet<>();
        for (Set<String> set : sets) {
            result.addAll(set);
        }
        return Set.copyOf(result);
    }
}
