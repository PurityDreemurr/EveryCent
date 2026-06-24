package com.everycent.assistant;

import com.everycent.assistant.dto.AccountingCaptureDTO;
import com.everycent.assistant.dto.AssistantResponseCardDTO;
import com.everycent.assistant.skill.AssistantPlan;
import com.everycent.assistant.skill.SkillResult;
import com.everycent.llm.dto.NaturalLanguageTransactionCreateResultDTO;
import com.everycent.service.dto.TransactionRecordDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResponseRenderer {

    public RenderedAssistantResponse render(AssistantPlan plan, List<SkillResult> results) {
        List<SkillResult> safeResults = results == null ? List.of() : results;
        List<AssistantResponseCardDTO> cards = new ArrayList<>();
        for (SkillResult result : safeResults) {
            cards.add(cardFor(result));
        }
        String message = messageFor(plan, safeResults);
        String responseType = cards.isEmpty() ? "message" : cards.get(0).getType();
        return new RenderedAssistantResponse(message, responseType, cards, accountingCapture(safeResults));
    }

    private AssistantResponseCardDTO cardFor(SkillResult result) {
        if (Boolean.TRUE.equals(result.getBlockedByPolicy())) {
            return new AssistantResponseCardDTO("policy_blocked", "不能由 AI 执行", blockedMessage(result), null);
        }
        if (Boolean.TRUE.equals(result.getNeedUserConfirmation())) {
            return new AssistantResponseCardDTO("confirmation_required", "需要确认", result.getMessage(), null);
        }
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            return new AssistantResponseCardDTO("error", "执行失败", result.getMessage(), null);
        }
        return switch (result.getActionName()) {
            case "transaction.create_from_text", "transaction.create" -> new AssistantResponseCardDTO(
                "transaction_created",
                "记账成功",
                "这笔记录已经入账。",
                result.getData()
            );
            case "transaction.list", "budget.status", "dashboard.summary", "dashboard.behavior_tags", "dashboard.emotion_tags" -> new AssistantResponseCardDTO(
                "query_result",
                "查询结果",
                "已查到相关结果。",
                result.getData()
            );
            case "budget.create", "budget.update" -> new AssistantResponseCardDTO("budget_saved", "预算已保存", "预算设置已经生效。", result.getData());
            case "export.transactions" -> new AssistantResponseCardDTO("download_result", "导出已准备", "账单导出已准备好。", result.getData());
            case "ledger.list" -> new AssistantResponseCardDTO("confirmation_required", "请选择账本", "请先选择要使用的账本。", result.getData());
            default -> new AssistantResponseCardDTO("result", "执行结果", "操作已完成。", result.getData());
        };
    }

    private String messageFor(AssistantPlan plan, List<SkillResult> results) {
        if (results.isEmpty()) {
            return withState("我在，喵。你可以直接告诉我要查账、记账、设置预算，或者先聊聊喵。", 40, "calm");
        }
        SkillResult first = results.get(0);
        if (Boolean.TRUE.equals(first.getBlockedByPolicy())) {
            return blockedMessage(first);
        }
        if (Boolean.TRUE.equals(first.getNeedUserConfirmation())) {
            return withState(first.getMessage() == null ? "这个行动还需要你确认一下喵。" : ensureMeow(first.getMessage()), 42, "calm");
        }
        if (!Boolean.TRUE.equals(first.getSuccess())) {
            return withState(
                first.getMessage() == null ? "这次行动没有执行成功，先检查一下信息是不是完整喵。" : ensureMeow(first.getMessage()),
                45,
                "tired"
            );
        }
        return switch (first.getActionName()) {
            case "transaction.create_from_text", "transaction.create" -> transactionCreatedMessage(first.getData());
            case "transaction.list" -> withState("账单查好了，明细我已经排在下面喵。", 42, "pleased");
            case "budget.create", "budget.update" -> withState("预算已经设置好了，这一步算是稳稳落地喵。", 45, "pleased");
            case "budget.status" -> withState("预算状态查好了，数字我放在下面喵。", 42, "calm");
            case "export.transactions" -> withState("导出结果准备好了，链接在下面喵。", 42, "calm");
            default -> withState("操作已经完成喵。", 40, "calm");
        };
    }

    private String blockedMessage(SkillResult result) {
        String actionName = result.getActionName();
        if (actionName != null && actionName.contains("delete")) {
            return withState("删除这类动作我不能直接替你下手，爪子得收住喵。你可以先让我查出候选记录，再到交易详情页或账单列表页手动删除喵。", 48, "calm");
        }
        if (actionName != null && actionName.startsWith("account.")) {
            return withState("账号和权限这种要紧地方，我不能替你代办喵。请到账号设置页面自己确认后再处理喵。", 48, "calm");
        }
        if ("assistant.technical_help".equals(actionName)) {
            return withState("写代码、程序实现、调试和算法题这类技术行动，我不能接招喵。你可以继续让我帮你记账、查账单、看预算或导出账单喵。", 46, "calm");
        }
        return withState(result.getMessage() == null ? "这个操作我不能执行，先把爪子收住喵。" : ensureMeow(result.getMessage()), 45, "calm");
    }

    private AccountingCaptureDTO accountingCapture(List<SkillResult> results) {
        AccountingCaptureDTO dto = new AccountingCaptureDTO();
        dto.setCaptured(false);
        dto.setCreated(false);
        dto.setNeedConfirmation(false);
        for (SkillResult result : results) {
            if (!"transaction.create_from_text".equals(result.getActionName()) && !"transaction.create".equals(result.getActionName())) {
                continue;
            }
            dto.setCaptured(true);
            dto.setCreated(Boolean.TRUE.equals(result.getSuccess()));
            dto.setNeedConfirmation(Boolean.TRUE.equals(result.getNeedUserConfirmation()));
            applyTransactionData(dto, result.getData());
            break;
        }
        return dto;
    }

    private void applyTransactionData(AccountingCaptureDTO dto, Object data) {
        if (data instanceof List<?> records && !records.isEmpty()) {
            applyTransactionData(dto, records.get(0));
            return;
        }
        if (data instanceof NaturalLanguageTransactionCreateResultDTO created) {
            dto.setTransactionId(created.getTransactionId());
            dto.setAmount(created.getAmount());
            dto.setType(created.getType());
            return;
        }
        if (data instanceof TransactionRecordDTO record) {
            dto.setTransactionId(record.getId());
            dto.setAmount(record.getAmount());
            dto.setType(record.getType());
            dto.setBehaviorTagCode(record.getBehaviorTagName());
            dto.setEmotionTagCode(record.getEmotionTagName());
            dto.setTransactionDate(record.getTransactionDate());
        }
    }

    private String transactionCreatedMessage(Object data) {
        if (data instanceof List<?> records && records.size() > 1) {
            return withState("已记账 " + records.size() + " 笔，这几笔我都按计划收好了喵。", 48, "pleased");
        }
        return withState("已记账，这笔我收好了喵。", 45, "pleased");
    }

    private String ensureMeow(String message) {
        if (message == null || message.contains("喵")) {
            return message;
        }
        String trimmed = message.trim();
        if (trimmed.endsWith("。") || trimmed.endsWith("！") || trimmed.endsWith("？")) {
            return trimmed.substring(0, trimmed.length() - 1) + "喵。";
        }
        return trimmed + "喵。";
    }

    private String withState(String message, int mood, String emoji) {
        return ensureMeow(message) + " {\"mood\":" + mood + ",\"emoji\":\"" + emoji + "\"}";
    }

    public record RenderedAssistantResponse(
        String assistantMessage,
        String responseType,
        List<AssistantResponseCardDTO> cards,
        AccountingCaptureDTO accountingCapture
    ) {}
}
