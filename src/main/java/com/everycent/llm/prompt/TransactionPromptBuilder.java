package com.everycent.llm.prompt;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TransactionPromptBuilder {

    public String build(
        String userInput,
        Ledger ledger,
        List<BehaviorTag> behaviorTags,
        List<EmotionTag> emotionTags,
        LocalDate defaultDate
    ) {
        return """
            你是 EveryCent 记账解析器，只输出 JSON。
            根据用户输入提取一条收支记录。

            用户输入：%s
            账本名称：%s
            默认日期：%s
            行为标签白名单：%s
            情绪标签白名单：%s

            JSON 格式：
            {
              "amount": "50.00",
              "type": "EXPENSE",
              "behaviorTagCode": "FOOD",
              "emotionTagCode": "HAPPY",
              "transactionDate": "2026-06-15",
              "description": "中午吃饭",
              "confidence": 0.92,
              "needUserConfirm": false
            }

            规则：
            - type 只能是 INCOME 或 EXPENSE。
            - amount 必须为正数。
            - 标签只能从白名单中选。
            - 日期不确定时用默认日期。
            - 无法确定金额、类型、日期、标签时，needUserConfirm=true 且 confidence 降低。
            - 兼容旧字段：behaviorTag=behaviorTagCode，moodTag=emotionTagCode，remark=description，needsManualReview=needUserConfirm。
            - 不要输出解释、Markdown 或多余文本。
            """.formatted(
                sanitize(userInput),
                ledger == null ? "" : sanitize(ledger.getName()),
                defaultDate,
                formatBehaviorTags(behaviorTags),
                formatEmotionTags(emotionTags)
            );
    }

    private String formatBehaviorTags(List<BehaviorTag> behaviorTags) {
        if (behaviorTags == null || behaviorTags.isEmpty()) {
            return "[]";
        }
        return behaviorTags
            .stream()
            .filter(tag -> StringUtils.hasText(tag.getCode()))
            .sorted(Comparator.comparing(BehaviorTag::getCode))
            .map(tag -> tag.getCode() + "(" + sanitize(tag.getName()) + ")")
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatEmotionTags(List<EmotionTag> emotionTags) {
        if (emotionTags == null || emotionTags.isEmpty()) {
            return "[]";
        }
        return emotionTags
            .stream()
            .filter(tag -> StringUtils.hasText(tag.getCode()))
            .sorted(Comparator.comparing(EmotionTag::getCode))
            .map(tag -> tag.getCode() + "(" + sanitize(tag.getName()) + ")")
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
