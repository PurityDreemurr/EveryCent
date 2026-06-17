package com.everycent.llm.prompt;

import com.everycent.domain.TransactionRecord;
import com.everycent.llm.dto.BudgetStatusDTO;
import com.everycent.llm.dto.EmotionStatDTO;
import com.everycent.domain.Ledger;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AlertPromptBuilder {

    public String build(
        Ledger ledger,
        BudgetStatusDTO budgetStatus,
        List<TransactionRecord> recentRecords,
        List<EmotionStatDTO> emotionStats
    ) {
        return """
            你是 EveryCent 的预算提醒文案生成器。
            请基于后端传入的预算状态、近期消费和情绪统计，生成温和、克制、可执行的预算提醒。

            输入信息：
            - 账本名称：%s
            - 预算金额：%s
            - 已使用金额：%s
            - 剩余金额：%s
            - 使用比例：%s
            - 近期消费：%s
            - 情绪统计：%s

            输出 JSON 格式必须严格为：
            {
              "title": "本月预算即将超支",
              "content": "你本月餐饮和娱乐消费较集中，预算已使用 82%%。建议接下来几天减少非必要支出。",
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
            """.formatted(
                ledger == null ? "" : sanitize(ledger.getName()),
                budgetStatus == null ? "" : budgetStatus.getLimitAmount(),
                budgetStatus == null ? "" : budgetStatus.getUsedAmount(),
                budgetStatus == null ? "" : budgetStatus.getRemainingAmount(),
                budgetStatus == null ? "" : budgetStatus.getUsedRatio(),
                formatRecentRecords(recentRecords),
                formatEmotionStats(emotionStats)
            );
    }

    private String formatRecentRecords(List<TransactionRecord> recentRecords) {
        if (recentRecords == null || recentRecords.isEmpty()) {
            return "[]";
        }
        return recentRecords
            .stream()
            .limit(10)
            .map(record ->
                "%s %s %s %s".formatted(
                        record.getTransactionDate(),
                        record.getType(),
                        record.getAmount(),
                        sanitize(record.getDescription())
                    )
            )
            .collect(Collectors.joining("; ", "[", "]"));
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
}
