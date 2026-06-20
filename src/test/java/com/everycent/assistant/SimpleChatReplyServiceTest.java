package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimpleChatReplyServiceTest {

    private final SimpleChatReplyService service = new SimpleChatReplyService();

    @Test
    void shouldReplyToCommonIntroductoryChat() {
        assertThat(service.reply("你好")).contains("EveryCent").doesNotContain("好，那就先这样");
        assertThat(service.reply("你是谁")).contains("AI 助手").doesNotContain("好，那就先这样");
        assertThat(service.reply("你会做什么")).contains("记账").contains("预算");
        assertThat(service.reply("能聊天吗")).contains("可以");
    }

    @Test
    void shouldReturnNullForUnmatchedDailyChatSoLlmCanHandleIt() {
        assertThat(service.reply("我今天有点无聊")).isNull();
    }
}
