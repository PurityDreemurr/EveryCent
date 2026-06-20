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
            return "我在。你可以直接告诉我要查账、记账、设置预算，或者先聊聊。";
        }
        SkillResult first = results.get(0);
        if (Boolean.TRUE.equals(first.getBlockedByPolicy())) {
            return blockedMessage(first);
        }
        if (Boolean.TRUE.equals(first.getNeedUserConfirmation())) {
            return first.getMessage();
        }
        if (!Boolean.TRUE.equals(first.getSuccess())) {
            return first.getMessage() == null ? "这次没有执行成功，请检查信息是否完整。" : first.getMessage();
        }
        return switch (first.getActionName()) {
            case "transaction.create_from_text", "transaction.create" -> transactionCreatedMessage(first.getData());
            case "transaction.list" -> "已查询账单。";
            case "budget.create", "budget.update" -> "预算已设置。";
            case "budget.status" -> "已查询预算状态。";
            case "export.transactions" -> "导出结果已准备。";
            default -> "已完成。";
        };
    }

    private String blockedMessage(SkillResult result) {
        String actionName = result.getActionName();
        if (actionName != null && actionName.contains("delete")) {
            return "删除类操作不能由 AI 助手执行。我可以先帮你查出候选记录，请到交易详情页或账单列表页手动删除。";
        }
        if (actionName != null && actionName.startsWith("account.")) {
            return "账号和账户权限相关操作不能由 AI 助手代办。请到账号设置页面手动处理。";
        }
        return result.getMessage() == null ? "这个操作不能由 AI 助手执行。" : result.getMessage();
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
            return "已记账 " + records.size() + " 笔。";
        }
        return "已记账。";
    }

    public record RenderedAssistantResponse(
        String assistantMessage,
        String responseType,
        List<AssistantResponseCardDTO> cards,
        AccountingCaptureDTO accountingCapture
    ) {}
}
