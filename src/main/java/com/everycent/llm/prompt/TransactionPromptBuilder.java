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
            你是 EveryCent 的自然语言记账解析器。
            请根据用户输入解析一条收支记录，并且只返回 JSON。

            输入信息：
            - 用户输入：%s
            - 账本名称：%s
            - 默认日期：%s
            - 行为标签白名单：%s
            - 情绪标签白名单：%s

            输出 JSON 格式必须严格为：
            {
              "amount": "50.00",
              "type": "EXPENSE",
              "behaviorTagCode": "FOOD",
              "emotionTagCode": "HAPPY",
              "transactionDate": "2026-06-15",
              "description": "中午吃饭",
              "confidence": 0.92
            }

            规则：
            1. type 只能为 INCOME 或 EXPENSE。
            2. behaviorTagCode 必须来自 behavior_tag.code 白名单。
            3. emotionTagCode 必须来自 emotion_tag.code 白名单。
            4. amount 必须为正数。
            5. 如果无法确定日期，使用默认日期。
            6. 如果无法确定行为标签，使用 OTHER 或系统设计中的默认标签。
            7. 不允许输出解释性文本。
            8. 不允许输出 Markdown。
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
