package com.everycent.llm.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.domain.Ledger;
import com.everycent.domain.TransactionRecord;
import com.everycent.domain.enumeration.TransactionType;
import com.everycent.llm.dto.BudgetStatusDTO;
import com.everycent.llm.dto.EmotionStatDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertPromptBuilderTest {

    private final AlertPromptBuilder builder = new AlertPromptBuilder();

    @Test
    void shouldBuildAlertPromptWithBudgetStatusRecentRecordsAndEmotionStats() {
        Ledger ledger = new Ledger().name("家庭账本");
        BudgetStatusDTO budgetStatus = new BudgetStatusDTO();
        budgetStatus.setLimitAmount(new BigDecimal("1000.00"));
        budgetStatus.setUsedAmount(new BigDecimal("820.00"));
        budgetStatus.setRemainingAmount(new BigDecimal("180.00"));
        budgetStatus.setUsedRatio(new BigDecimal("0.82"));

        TransactionRecord record = new TransactionRecord()
            .transactionDate(LocalDate.of(2026, 6, 15))
            .type(TransactionType.EXPENSE)
            .amount(new BigDecimal("50.00"))
            .description("中午吃饭");

        EmotionStatDTO emotionStat = new EmotionStatDTO();
        emotionStat.setEmotionTagCode("HAPPY");
        emotionStat.setEmotionTagName("开心");
        emotionStat.setCount(3L);
        emotionStat.setRatio(new BigDecimal("0.60"));

        String prompt = builder.build(ledger, budgetStatus, List.of(record), List.of(emotionStat));

        assertThat(prompt).contains("预算提醒文案生成器");
        assertThat(prompt).contains("账本名称：家庭账本");
        assertThat(prompt).contains("预算金额：1000.00");
        assertThat(prompt).contains("已使用金额：820.00");
        assertThat(prompt).contains("剩余金额：180.00");
        assertThat(prompt).contains("使用比例：0.82");
        assertThat(prompt).contains("2026-06-15 EXPENSE 50.00 中午吃饭");
        assertThat(prompt).contains("HAPPY(开心): count=3, ratio=0.60");
        assertThat(prompt).contains("\"level\": \"WARNING\"");
        assertThat(prompt).contains("\"needNotification\": true");
        assertThat(prompt).contains("level 只能为 INFO、WARNING、DANGER");
        assertThat(prompt).contains("文案不得包含羞辱、责备、极端化表达");
        assertThat(prompt).contains("不允许编造金额");
        assertThat(prompt).contains("不允许输出 Markdown");
    }
}
