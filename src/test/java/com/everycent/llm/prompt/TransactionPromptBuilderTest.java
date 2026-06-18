package com.everycent.llm.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.domain.BehaviorTag;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.Ledger;
import com.everycent.domain.enumeration.EmotionValence;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionPromptBuilderTest {

    private final TransactionPromptBuilder builder = new TransactionPromptBuilder();

    @Test
    void shouldBuildPromptStrictlyFollowingDocumentRules() {
        Ledger ledger = new Ledger().name("日常账本");
        BehaviorTag food = new BehaviorTag().code("FOOD").name("餐饮");
        BehaviorTag other = new BehaviorTag().code("OTHER").name("其他");
        EmotionTag happy = new EmotionTag().code("HAPPY").name("开心").valence(EmotionValence.POSITIVE);

        String prompt = builder.build("今天午饭 50 元，很开心", ledger, List.of(food, other), List.of(happy), LocalDate.of(2026, 6, 15));

        assertThat(prompt).contains("自然语言记账解析器");
        assertThat(prompt).contains("用户输入：今天午饭 50 元，很开心");
        assertThat(prompt).contains("账本名称：日常账本");
        assertThat(prompt).contains("默认日期：2026-06-15");
        assertThat(prompt).contains("FOOD(餐饮)", "OTHER(其他)", "HAPPY(开心)");
        assertThat(prompt).contains("\"amount\": \"50.00\"");
        assertThat(prompt).contains("\"type\": \"EXPENSE\"");
        assertThat(prompt).contains("\"needUserConfirm\": false");
        assertThat(prompt).contains("type 只能为 INCOME 或 EXPENSE");
        assertThat(prompt).contains("behaviorTagCode 必须来自 behavior_tag.code 白名单");
        assertThat(prompt).contains("emotionTagCode 必须来自 emotion_tag.code 白名单");
        assertThat(prompt).contains("amount 必须为正数");
        assertThat(prompt).contains("needUserConfirm 设为 true");
        assertThat(prompt).contains("兼容旧字段含义");
        assertThat(prompt).contains("不允许输出解释性文本");
        assertThat(prompt).contains("不允许输出 Markdown");
    }
}
