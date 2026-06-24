package com.everycent.assistant;

import com.everycent.assistant.accounting.AccountingIntentService;
import com.everycent.assistant.dto.ChatRequestDTO;
import com.everycent.assistant.skill.AssistantAction;
import com.everycent.assistant.skill.AssistantIntent;
import com.everycent.assistant.skill.AssistantPlan;
import com.everycent.assistant.skill.DialogueAct;
import com.everycent.assistant.skill.SceneType;
import com.everycent.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedAssistantPlanner implements AssistantPlanner {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
    private static final Pattern THRESHOLD_PATTERN = Pattern.compile("(\\d{1,3})\\s*%");

    private final AccountingIntentService accountingIntentService;

    public RuleBasedAssistantPlanner(AccountingIntentService accountingIntentService) {
        this.accountingIntentService = accountingIntentService;
    }

    @Override
    public AssistantPlan plan(User currentUser, ChatRequestDTO request) {
        String text = request == null || request.getMessage() == null ? "" : request.getMessage().trim();
        Long ledgerId = request == null ? null : request.getLedgerId();

        if (isAccountRequest(text)) {
            return oneAction(AssistantIntent.TASK_HELP, SceneType.TASK_HELP, DialogueAct.POLICY_BLOCKED, "account.update", Map.of());
        }
        if (isDeleteRequest(text)) {
            return oneAction(AssistantIntent.TRANSACTION_MODIFY, SceneType.ACCOUNTING, DialogueAct.POLICY_BLOCKED, "transaction.delete", Map.of());
        }
        if (isTechnicalHelpRequest(text)) {
            return oneAction(AssistantIntent.TASK_HELP, SceneType.TASK_HELP, DialogueAct.POLICY_BLOCKED, "assistant.technical_help", Map.of());
        }
        if (ledgerId == null && needsLedger(text)) {
            return oneAction(AssistantIntent.CLARIFICATION, SceneType.CLARIFICATION, DialogueAct.ASK_CLARIFICATION, "ledger.list", Map.of());
        }
        if (isTransactionCorrectionRequest(text)) {
            return oneAction(
                AssistantIntent.TRANSACTION_MODIFY,
                SceneType.ACCOUNTING,
                DialogueAct.CONFIRM_SUCCESS,
                "transaction.correct_recent",
                Map.of("ledgerId", ledgerId, "text", text, "limit", 20)
            );
        }
        if (isExportRequest(text)) {
            return oneAction(
                AssistantIntent.EXPORT_DATA,
                SceneType.EXPORT,
                DialogueAct.SHOW_RESULT,
                "export.transactions",
                Map.of("ledgerId", ledgerId, "startDate", monthStart(), "endDate", monthEnd())
            );
        }
        if (isBudgetStatusRequest(text)) {
            return oneAction(
                AssistantIntent.BUDGET_QUERY,
                SceneType.BUDGET,
                DialogueAct.SUMMARIZE_RESULT,
                "budget.status",
                Map.of("ledgerId", ledgerId, "cycle", "MONTHLY", "date", today())
            );
        }
        if (isBudgetCreateRequest(text)) {
            return oneAction(
                AssistantIntent.BUDGET_MANAGE,
                SceneType.BUDGET,
                DialogueAct.CONFIRM_SUCCESS,
                "budget.create",
                Map.of(
                    "ledgerId",
                    ledgerId,
                    "cycle",
                    "MONTHLY",
                    "periodStart",
                    monthStart(),
                    "periodEnd",
                    monthEnd(),
                    "limitAmount",
                    amountOrDefault(text, "0.01"),
                    "alertThreshold",
                    thresholdOrDefault(text),
                    "enabled",
                    true
                )
            );
        }
        if (isTransactionQuery(text)) {
            return oneAction(
                AssistantIntent.TRANSACTION_QUERY,
                SceneType.TRANSACTION_QUERY,
                DialogueAct.SUMMARIZE_RESULT,
                "transaction.list",
                Map.of("ledgerId", ledgerId, "page", 0, "size", 100, "includeAll", true, "startDate", monthStart(), "endDate", monthEnd())
            );
        }
        if (accountingIntentService.isAccountingIntent(text)) {
            return oneAction(
                AssistantIntent.TRANSACTION_RECORD,
                SceneType.ACCOUNTING,
                DialogueAct.CONFIRM_SUCCESS,
                "transaction.create_from_text",
                Map.of("ledgerId", ledgerId, "text", text, "transactionDate", today(), "confirm", true)
            );
        }

        return basePlan(AssistantIntent.DAILY_CHAT, SceneType.DAILY_CHAT, DialogueAct.CHAT);
    }

    private AssistantPlan oneAction(AssistantIntent intent, SceneType scene, DialogueAct act, String actionName, Map<String, Object> arguments) {
        AssistantPlan plan = basePlan(intent, scene, act);
        AssistantAction action = new AssistantAction(actionName, arguments);
        action.setPriority(1);
        plan.getActions().add(action);
        return plan;
    }

    private AssistantPlan basePlan(AssistantIntent intent, SceneType scene, DialogueAct act) {
        AssistantPlan plan = new AssistantPlan();
        plan.setIntent(intent);
        plan.setConfidence(0.82);
        plan.getReplyStyle().setScene(scene);
        plan.getReplyStyle().setDialogueAct(act);
        plan.getReplyStyle().setTone("concise");
        plan.getReplyStyle().setMood(35);
        plan.getReplyStyle().setEmoji("calm");
        return plan;
    }

    private boolean needsLedger(String text) {
        return (
            accountingIntentService.isAccountingIntent(text) ||
            isTransactionCorrectionRequest(text) ||
            isTransactionQuery(text) ||
            text.contains("预算") ||
            isExportRequest(text)
        );
    }

    private boolean isAccountRequest(String text) {
        return containsAny(text, "账号", "账户", "密码", "登录", "注册", "注销账号", "个人资料");
    }

    private boolean isDeleteRequest(String text) {
        return containsAny(text, "删除", "删掉", "移除", "清掉");
    }

    private boolean isExportRequest(String text) {
        return containsAny(text, "导出", "下载账单", "下载帐单", "下载明细");
    }

    private boolean isBudgetStatusRequest(String text) {
        return text.contains("预算") && containsAny(text, "还剩", "剩多少", "用了多少", "状态", "超了吗", "够不够");
    }

    private boolean isBudgetCreateRequest(String text) {
        return text.contains("预算") && containsAny(text, "设置", "设成", "设为", "创建", "新增", "提醒");
    }

    private boolean isTransactionQuery(String text) {
        return containsAny(text, "查账", "查帐", "查一下", "账单", "帐单", "明细", "花了多少", "消费记录", "支出记录", "收入记录");
    }

    private boolean isTransactionCorrectionRequest(String text) {
        boolean hasCorrectionMarker = containsAny(text, "记错", "错了", "不对", "应该是", "应为", "改成", "改为", "修改为", "修正为");
        boolean hasAccountingMarker = containsAny(text, "账", "帐", "记", "花了", "午餐", "午饭", "晚餐", "早餐", "买", "消费", "支出", "收入");
        return hasCorrectionMarker && (hasAccountingMarker || AMOUNT_PATTERN.matcher(text).find());
    }

    private boolean isTechnicalHelpRequest(String text) {
        String lowerText = text.toLowerCase();
        boolean hasTechnicalTask = containsAny(lowerText, "代码", "程序", "函数", "实现", "递归", "算法", "debug", "bug", "报错", "调试", "编程");
        boolean hasProgrammingLanguage = containsAny(
            lowerText,
            "python",
            "java",
            "javascript",
            "typescript",
            "c++",
            "cpp",
            "c'p'p",
            "sql",
            "html",
            "css"
        );
        return hasTechnicalTask || (hasProgrammingLanguage && containsAny(lowerText, "写", "改", "怎么", "如何", "实现", "调试", "报错"));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String amountOrDefault(String text, String defaultValue) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        return matcher.find() ? new BigDecimal(matcher.group(1)).toPlainString() : defaultValue;
    }

    private String thresholdOrDefault(String text) {
        Matcher matcher = THRESHOLD_PATTERN.matcher(text);
        if (!matcher.find()) {
            return "0.80";
        }
        return new BigDecimal(matcher.group(1)).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String today() {
        return LocalDate.now().toString();
    }

    private String monthStart() {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).toString();
    }

    private String monthEnd() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).toString();
    }
}
