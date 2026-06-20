package com.everycent.assistant.skill;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ActionSchemaValidator {

    private static final Pattern ACTION_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)^(https?://|/api/).*");
    private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("^[a-z]+(\\.[a-z_$][\\w$]*)*\\.[A-Z_$][\\w$]*(\\.[a-zA-Z_$][\\w$]*)*$");
    private static final java.util.Set<String> SQL_KEYWORDS = java.util.Set.of(
        "select",
        "insert",
        "update",
        "delete",
        "drop",
        "alter",
        "truncate",
        "create"
    );

    private static final Map<String, List<String>> REQUIRED_ARGUMENTS = Map.ofEntries(
        Map.entry("ledger.create", List.of("name")),
        Map.entry("ledger.get", List.of("ledgerId")),
        Map.entry("ledger.update", List.of("ledgerId", "name")),
        Map.entry("ledger.member.list", List.of("ledgerId")),
        Map.entry("ledger.member.add", List.of("ledgerId", "userId", "permissionLevel")),
        Map.entry("ledger.member.update", List.of("ledgerId", "userId", "permissionLevel")),
        Map.entry("transaction.list", List.of("ledgerId")),
        Map.entry("transaction.get", List.of("transactionId")),
        Map.entry("transaction.parse", List.of("ledgerId", "text")),
        Map.entry("transaction.create", List.of("ledgerId", "amount", "type", "recordDate")),
        Map.entry("transaction.create_from_text", List.of("ledgerId", "text", "confirm")),
        Map.entry("transaction.update", List.of("transactionId")),
        Map.entry("budget.list", List.of("ledgerId")),
        Map.entry("budget.create", List.of("ledgerId", "cycle", "periodStart", "periodEnd", "limitAmount")),
        Map.entry("budget.update", List.of("budgetId", "cycle", "periodStart", "periodEnd", "limitAmount")),
        Map.entry("budget.status", List.of("ledgerId")),
        Map.entry("budget.alert.generate", List.of("ledgerId", "budgetId")),
        Map.entry("dashboard.summary", List.of("ledgerId")),
        Map.entry("dashboard.trend", List.of("ledgerId")),
        Map.entry("dashboard.behavior_tags", List.of("ledgerId")),
        Map.entry("dashboard.emotion_tags", List.of("ledgerId")),
        Map.entry("export.transactions", List.of("ledgerId", "startDate", "endDate"))
    );

    public void validate(AssistantAction action) {
        if (action == null) {
            throw new InvalidActionException("Action 不能为空");
        }
        validateActionName(action.getName());
        validateRequiredArguments(action);
        validateActionSpecificRules(action);
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
        List<String> requiredArguments = REQUIRED_ARGUMENTS.get(action.getName());
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

    private void validateActionSpecificRules(AssistantAction action) {
        if (!"transaction.create_from_text".equals(action.getName())) {
            return;
        }
        if (!Boolean.TRUE.equals(new ActionArgumentReader(action).booleanValue("confirm"))) {
            throw new InvalidActionException("Action transaction.create_from_text 必须由用户确认 confirm=true 后才能执行");
        }
    }
}
