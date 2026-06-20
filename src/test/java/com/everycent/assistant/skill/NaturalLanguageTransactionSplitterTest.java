package com.everycent.assistant.skill;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NaturalLanguageTransactionSplitterTest {

    private final NaturalLanguageTransactionSplitter splitter = new NaturalLanguageTransactionSplitter();

    @Test
    void shouldSplitMultipleShortTransactionItems() {
        assertThat(splitter.split("午饭28，咖啡18，分成两笔流水记录")).containsExactly("午饭28", "咖啡18");
        assertThat(splitter.split("午饭28、地铁6、奶茶12")).containsExactly("午饭28", "地铁6", "奶茶12");
    }

    @Test
    void shouldCarryObjectContextToFollowingAmountOnlyPart() {
        assertThat(splitter.split("我今天晚上去夜市，买生蚝花了20元，然后又吃了海鲜，花了62"))
            .containsExactly("今天晚上生蚝20元", "今天晚上海鲜62");
    }

    @Test
    void shouldCarrySharedAmountToFollowingSpendWithoutAmount() {
        assertThat(splitter.split("昨天开工资收入50元，修手机都花了")).containsExactly("昨天开工资收入50元", "昨天修手机花了50元");
    }

    @Test
    void shouldCarryDateContextToFollowingTransactions() {
        assertThat(splitter.split("昨天开工资收入50元，修手机花了50")).containsExactly("昨天开工资收入50元", "昨天修手机50");
    }

    @Test
    void shouldKeepSingleTransactionTextAsOneRecord() {
        assertThat(splitter.split("午饭28")).containsExactly("午饭28");
        assertThat(splitter.split("外卖花了28，结果送错了")).containsExactly("外卖花了28，结果送错了");
    }
}
