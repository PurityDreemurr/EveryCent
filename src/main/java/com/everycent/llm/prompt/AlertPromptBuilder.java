package com.everycent.llm.prompt;

import com.everycent.domain.TransactionRecord;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.BudgetStatusDTO;
import com.everycent.llm.dto.EmotionStatDTO;
import com.everycent.domain.Ledger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AlertPromptBuilder {

    public String build(
        Ledger ledger,
        BudgetStatusDTO budgetStatus,
        List<TransactionRecord> periodRecords,
        List<TransactionRecord> recentRecords,
        List<EmotionStatDTO> emotionStats
    ) {
        return """
            你是 EveryCent 的预算预警分析器。
            请只基于后端传入的预算状态和账目明细，生成个性化预算预警。
            你需要指出：
            - 本周期的大头花销是什么。
            - 哪些花销可能是不必要或可压缩的。
            - 接下来可以采取什么具体动作。

            输入信息：
            - 账本名称：%s
            - 预算周期：%s 至 %s
            - 预算金额：%s
            - 已使用金额：%s
            - 剩余金额：%s
            - 使用比例：%s
            - 本周期支出分类汇总：%s
            - 本周期最大支出明细：%s
            - 本周期完整账目样本：%s
            - 最近 14 天账目：%s
            - 情绪统计：%s

            输出 JSON 格式必须严格为：
            {
              "title": "本月预算即将超支",
              "content": "预算已使用 82%%，餐饮和娱乐是本期主要支出。接下来建议优先压缩外卖、奶茶等可选消费。",
              "analysisSummary": "本期支出主要集中在餐饮和娱乐，单笔较大的支出集中在外卖与聚餐。",
              "majorExpenses": ["餐饮 520.00 元，占支出 63.41%%", "娱乐 180.00 元，占支出 21.95%%"],
              "unnecessaryExpenses": ["多次奶茶/零食支出较集中，可考虑减少频次"],
              "suggestions": ["剩余周期优先控制餐饮外卖", "单次非必要消费尽量控制在 30 元以内"],
              "level": "WARNING",
              "needNotification": true
            }

            规则：
            1. level 只能为 INFO、WARNING、DANGER。
            2. 文案不得包含羞辱、责备、极端化表达。
            3. 文案必须基于后端传入的预算状态，不允许编造金额。
            4. 文案不直接决定是否入库。
            5. 是否生成通知由后端根据 needNotification 和预算状态决定。
            6. 不允许输出解释性文本。
            7. 不允许输出 Markdown。
            8. majorExpenses 必须来自“本周期支出分类汇总”或“本周期最大支出明细”。
            9. unnecessaryExpenses 只能使用“可能、可考虑、可压缩”等温和判断，不要武断评价。
            10. suggestions 最多 3 条，每条短句，必须可执行。
            11. 如果账目太少，明确说明“当前样本较少”，不要强行分析。
            """.formatted(
                ledger == null ? "" : sanitize(ledger.getName()),
                formatPeriodStart(periodRecords),
                formatPeriodEnd(periodRecords),
                budgetStatus == null ? "" : budgetStatus.getLimitAmount(),
                budgetStatus == null ? "" : budgetStatus.getUsedAmount(),
                budgetStatus == null ? "" : budgetStatus.getRemainingAmount(),
                budgetStatus == null ? "" : budgetStatus.getUsedRatio(),
                formatCategorySummary(periodRecords),
                formatLargestExpenses(periodRecords),
                formatPeriodRecords(periodRecords),
                formatRecentRecords(recentRecords),
                formatEmotionStats(emotionStats)
            );
    }

    private String formatPeriodStart(List<TransactionRecord> periodRecords) {
        return periodRecords == null || periodRecords.isEmpty()
            ? ""
            : periodRecords.stream().map(TransactionRecord::getTransactionDate).min(Comparator.naturalOrder()).map(Object::toString).orElse("");
    }

    private String formatPeriodEnd(List<TransactionRecord> periodRecords) {
        return periodRecords == null || periodRecords.isEmpty()
            ? ""
            : periodRecords.stream().map(TransactionRecord::getTransactionDate).max(Comparator.naturalOrder()).map(Object::toString).orElse("");
    }

    private String formatCategorySummary(List<TransactionRecord> periodRecords) {
        if (periodRecords == null || periodRecords.isEmpty()) {
            return "[]";
        }
        BigDecimal totalExpense = periodRecords
            .stream()
            .filter(record -> record.getType() == TransactionType.EXPENSE)
            .map(TransactionRecord::getAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return "[]";
        }
        Map<String, CategoryStat> stats = new LinkedHashMap<>();
        periodRecords
            .stream()
            .filter(record -> record.getType() == TransactionType.EXPENSE)
            .forEach(record -> {
                String category = record.getBehaviorTag() == null ? "未分类" : sanitize(record.getBehaviorTag().getName());
                CategoryStat stat = stats.computeIfAbsent(category, ignored -> new CategoryStat());
                stat.amount = stat.amount.add(record.getAmount() == null ? BigDecimal.ZERO : record.getAmount());
                stat.count++;
            });
        return stats
            .entrySet()
            .stream()
            .sorted((left, right) -> right.getValue().amount.compareTo(left.getValue().amount))
            .limit(8)
            .map(entry -> {
                BigDecimal ratio = entry.getValue().amount.divide(totalExpense, 4, RoundingMode.HALF_UP);
                return "%s: amount=%s, count=%s, ratio=%s".formatted(entry.getKey(), entry.getValue().amount, entry.getValue().count, ratio);
            })
            .collect(Collectors.joining("; ", "[", "]"));
    }

    private String formatLargestExpenses(List<TransactionRecord> periodRecords) {
        if (periodRecords == null || periodRecords.isEmpty()) {
            return "[]";
        }
        return periodRecords
            .stream()
            .filter(record -> record.getType() == TransactionType.EXPENSE)
            .sorted((left, right) -> nullSafeAmount(right).compareTo(nullSafeAmount(left)))
            .limit(10)
            .map(this::formatRecord)
            .collect(Collectors.joining("; ", "[", "]"));
    }

    private String formatPeriodRecords(List<TransactionRecord> periodRecords) {
        if (periodRecords == null || periodRecords.isEmpty()) {
            return "[]";
        }
        return periodRecords.stream().limit(60).map(this::formatRecord).collect(Collectors.joining("; ", "[", "]"));
    }

    private String formatRecentRecords(List<TransactionRecord> recentRecords) {
        if (recentRecords == null || recentRecords.isEmpty()) {
            return "[]";
        }
        return recentRecords.stream().limit(20).map(this::formatRecord).collect(Collectors.joining("; ", "[", "]"));
    }

    private String formatRecord(TransactionRecord record) {
        return "%s %s %s category=%s emotion=%s note=%s".formatted(
                record.getTransactionDate(),
                record.getType(),
                record.getAmount(),
                record.getBehaviorTag() == null ? "" : sanitize(record.getBehaviorTag().getName()),
                record.getEmotionTag() == null ? "" : sanitize(record.getEmotionTag().getName()),
                sanitize(record.getDescription())
            );
    }

    private BigDecimal nullSafeAmount(TransactionRecord record) {
        return record == null || record.getAmount() == null ? BigDecimal.ZERO : record.getAmount();
    }

    private String formatEmotionStats(List<EmotionStatDTO> emotionStats) {
        if (emotionStats == null || emotionStats.isEmpty()) {
            return "[]";
        }
        return emotionStats
            .stream()
            .map(stat ->
                "%s(%s): count=%s, ratio=%s".formatted(
                        sanitize(stat.getEmotionTagCode()),
                        sanitize(stat.getEmotionTagName()),
                        stat.getCount(),
                        stat.getRatio()
                    )
            )
            .collect(Collectors.joining("; ", "[", "]"));
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static class CategoryStat {

        private BigDecimal amount = BigDecimal.ZERO;
        private long count;
    }
}
