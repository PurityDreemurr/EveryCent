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
            .containsExactly("生蚝20元", "海鲜62");
    }

    @Test
    void shouldKeepSingleTransactionTextAsOneRecord() {
        assertThat(splitter.split("午饭28")).containsExactly("午饭28");
        assertThat(splitter.split("外卖花了28，结果送错了")).containsExactly("外卖花了28，结果送错了");
    }
}
