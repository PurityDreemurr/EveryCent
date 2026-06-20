package com.everycent.assistant.skill;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ActionSchemaValidator {

    private static final Pattern ACTION_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)^(https?://|/api/).*");
    private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("^[a-z]+(\\.[a-z_$][\\w$]*)*\\.[A-Z_$][\\w$]*(\\.[a-zA-Z_$][\\w$]*)*$");
    private static final Set<String> SQL_KEYWORDS = Set.of(
        "select",
        "insert",
        "update",
        "delete",
        "drop",
        "alter",
        "truncate",
        "create"
    );

    private static final Map<String, Set<String>> REQUIRED_ARGUMENTS = Map.ofEntries(
        Map.entry("ledger.get", Set.of("ledgerId")),
        Map.entry("ledger.update", Set.of("ledgerId")),
        Map.entry("ledger.member.list", Set.of("ledgerId")),
        Map.entry("ledger.member.add", Set.of("ledgerId", "userId", "permissionLevel")),
        Map.entry("ledger.member.update", Set.of("ledgerId", "userId", "permissionLevel")),
        Map.entry("transaction.list", Set.of("ledgerId")),
        Map.entry("transaction.get", Set.of("transactionId")),
        Map.entry("transaction.parse", Set.of("ledgerId", "text")),
        Map.entry("transaction.create", Set.of("ledgerId", "amount", "type")),
        Map.entry("transaction.create_from_text", Set.of("ledgerId", "text", "confirm")),
        Map.entry("transaction.update", Set.of("transactionId")),
        Map.entry("budget.list", Set.of("ledgerId")),
        Map.entry("budget.create", Set.of("ledgerId", "cycle", "limitAmount")),
        Map.entry("budget.update", Set.of("budgetId")),
        Map.entry("budget.status", Set.of("ledgerId")),
        Map.entry("budget.alert.generate", Set.of("ledgerId", "budgetId")),
        Map.entry("dashboard.summary", Set.of("ledgerId")),
        Map.entry("dashboard.trend", Set.of("ledgerId")),
        Map.entry("dashboard.behavior_tags", Set.of("ledgerId")),
        Map.entry("dashboard.emotion_tags", Set.of("ledgerId")),
        Map.entry("export.transactions", Set.of("ledgerId"))
    );

    public void validate(AssistantAction action) {
        if (action == null) {
            throw new InvalidActionException("Action 不能为空");
        }
        validateActionName(action.getName());
        validateRequiredArguments(action);
    }

    private void validateActionName(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            throw new InvalidActionException("Action name 不能为空");
        }
        if (URL_PATTERN.matcher(actionName).matches()) {
            throw new InvalidActionException("Action name 不能是 URL 或接口路径");
        }
        String lowerName = actionName.toLowerCase();
        if (SQL_KEYWORDS.contains(lowerName) || lowerName.startsWith("sql.") || lowerName.contains(".sql.")) {
            throw new InvalidActionException("Action name 不能是 SQL 或 JPQL 操作");
        }
        if (lowerName.contains("repository")) {
            throw new InvalidActionException("Action name 不能直接引用 Repository");
        }
        if (!ACTION_NAME_PATTERN.matcher(actionName).matches() || JAVA_CLASS_PATTERN.matcher(actionName).matches()) {
            throw new InvalidActionException("Action name 必须是 Skill 白名单命名格式");
        }
    }

    private void validateRequiredArguments(AssistantAction action) {
        Set<String> requiredArguments = REQUIRED_ARGUMENTS.get(action.getName());
        if (requiredArguments == null || requiredArguments.isEmpty()) {
            return;
        }
        Map<String, Object> arguments = action.getArguments();
        for (String requiredArgument : requiredArguments) {
            Object value = arguments.get(requiredArgument);
            if (value == null || (value instanceof String text && text.isBlank())) {
                throw new InvalidActionException("Action " + action.getName() + " 缺少必要参数：" + requiredArgument);
            }
        }
    }
}
