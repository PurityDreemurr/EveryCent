package com.everycent.qqbot;

import com.everycent.assistant.dto.AssistantResponseCardDTO;
import com.everycent.assistant.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class QqAssistantReplyRenderer {

    private final ObjectMapper objectMapper;

    public QqAssistantReplyRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(ChatResponseDTO response) {
        if (response == null) {
            return null;
        }
        StringBuilder text = new StringBuilder(clean(response.getAssistantMessage()));
        for (AssistantResponseCardDTO card : response.getCards()) {
            String renderedCard = renderCard(card);
            if (StringUtils.hasText(renderedCard) && !text.toString().contains(renderedCard)) {
                appendBlock(text, renderedCard);
            }
        }
        return text.toString();
    }

    private String renderCard(AssistantResponseCardDTO card) {
        if (card == null || card.getData() == null) {
            return null;
        }
        JsonNode data = objectMapper.valueToTree(card.getData());
        return switch (card.getType() == null ? "" : card.getType()) {
            case "transaction_created" -> renderCreated(data);
            case "query_result" -> renderQuery(data);
            case "budget_saved" -> renderBudgetSaved(data);
            case "download_result" -> renderDownload(data);
            default -> renderObjectSummary(data);
        };
    }

    private String renderCreated(JsonNode data) {
        List<JsonNode> records = records(data);
        if (records.isEmpty() && data.isObject()) {
            records = List.of(data);
        }
        if (records.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        lines.add("入账明细：");
        int index = 1;
        for (JsonNode record : records) {
            lines.add(index++ + ". " + transactionLine(record));
        }
        return String.join("\n", lines);
    }

    private String renderQuery(JsonNode data) {
        if (data == null || data.isNull()) {
            return null;
        }
        List<JsonNode> records = records(data);
        if (!records.isEmpty()) {
            return renderTransactions(data, records);
        }
        if (hasAny(data, "limitAmount", "usedAmount", "remainingAmount", "usedRatio", "budgetAmount")) {
            return renderBudgetStatus(data);
        }
        if (hasAny(data, "totalIncome", "incomeTotal", "totalExpense", "expenseTotal", "balance", "transactionCount")) {
            return renderSummary(data);
        }
        return renderObjectSummary(data);
    }

    private String renderTransactions(JsonNode data, List<JsonNode> records) {
        List<String> lines = new ArrayList<>();
        lines.add("账单明细：共 " + longValue(data, "totalElements", records.size()) + " 条");
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int index = 1;
        for (JsonNode record : records) {
            BigDecimal amount = amount(record, "amount");
            if ("INCOME".equalsIgnoreCase(text(record, "type", ""))) {
                income = income.add(amount);
            } else {
                expense = expense.add(amount);
            }
            lines.add(index + ". " + transactionLine(record));
            index++;
        }
        lines.add("收入合计：" + money(income));
        lines.add("支出合计：" + money(expense));
        return String.join("\n", lines);
    }

    private String renderBudgetStatus(JsonNode data) {
        List<String> lines = new ArrayList<>();
        lines.add("预算状态：");
        lines.add("预算额度：" + money(amount(data, "limitAmount", "budgetAmount", "amount")));
        lines.add("已使用：" + money(amount(data, "usedAmount")));
        lines.add("剩余：" + money(amount(data, "remainingAmount")));
        lines.add("使用率：" + percent(amount(data, "usedRatio", "usageRate")));
        String status = text(data, "status", null);
        if (StringUtils.hasText(status)) {
            lines.add("状态：" + status);
        }
        return String.join("\n", lines);
    }

    private String renderBudgetSaved(JsonNode data) {
        return "预算已保存：额度 " + money(amount(data, "limitAmount", "budgetAmount", "amount")) + "，提醒阈值 " + percent(amount(data, "alertThreshold"));
    }

    private String renderSummary(JsonNode data) {
        List<String> lines = new ArrayList<>();
        lines.add("收支概览：");
        lines.add("收入：" + money(amount(data, "totalIncome", "incomeTotal")));
        lines.add("支出：" + money(amount(data, "totalExpense", "expenseTotal")));
        lines.add("结余：" + money(amount(data, "balance")));
        lines.add("流水数：" + longValue(data, "transactionCount", 0) + " 条");
        if (hasAny(data, "budgetUsedRatio", "budgetUsedRate")) {
            lines.add("预算使用率：" + percent(amount(data, "budgetUsedRatio", "budgetUsedRate")));
        }
        return String.join("\n", lines);
    }

    private String renderDownload(JsonNode data) {
        String url = text(data, "downloadUrl", null);
        if (StringUtils.hasText(url)) {
            return "下载链接：" + url;
        }
        return renderObjectSummary(data);
    }

    private String renderObjectSummary(JsonNode data) {
        if (data == null || data.isNull() || !data.isObject()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        data
            .fields()
            .forEachRemaining(entry -> {
                if (!entry.getValue().isObject() && !entry.getValue().isArray()) {
                    lines.add(entry.getKey() + "：" + entry.getValue().asText());
                }
            });
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String transactionLine(JsonNode record) {
        String type = "INCOME".equalsIgnoreCase(text(record, "type", "")) ? "收入" : "支出";
        String date = text(record, "recordDate", text(record, "transactionDate", "-"));
        String name = text(record, "description", text(record, "rawInput", text(record, "behaviorTagName", text(record, "behaviorTag", "未命名"))));
        String tag = text(record, "behaviorTagName", text(record, "behaviorTag", "未分类"));
        return date + " " + type + " " + name + " " + money(amount(record, "amount")) + " [" + tag + "]";
    }

    private List<JsonNode> records(JsonNode data) {
        if (data == null || data.isNull()) {
            return List.of();
        }
        JsonNode content = data.path("content");
        if (content.isArray()) {
            return toList(content);
        }
        if (data.isArray()) {
            return toList(data);
        }
        return List.of();
    }

    private List<JsonNode> toList(JsonNode array) {
        List<JsonNode> values = new ArrayList<>();
        array.forEach(values::add);
        return values;
    }

    private boolean hasAny(JsonNode data, String... fields) {
        if (data == null) {
            return false;
        }
        for (String field : fields) {
            if (!data.path(field).isMissingNode() && !data.path(field).isNull()) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal amount(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node == null ? null : node.path(field);
            if (value != null && !value.isMissingNode() && !value.isNull() && StringUtils.hasText(value.asText())) {
                try {
                    return new BigDecimal(value.asText());
                } catch (NumberFormatException ignored) {
                    return BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private long longValue(JsonNode node, String field, long defaultValue) {
        JsonNode value = node == null ? null : node.path(field);
        return value != null && value.canConvertToLong() ? value.asLong() : defaultValue;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node == null ? null : node.path(field);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private String money(BigDecimal value) {
        return "¥" + (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String clean(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        return message.replaceAll("\\s*\\{\\s*\"mood\"\\s*:\\s*\\d+\\s*,\\s*\"emoji\"\\s*:\\s*\"[^\"]+\"\\s*}\\s*$", "").trim();
    }

    private void appendBlock(StringBuilder builder, String block) {
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(block);
    }
}
