package com.everycent.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.everycent.assistant.rewrite.LlmRewriteService;
import com.everycent.assistant.validation.DialogueScene;
import com.everycent.assistant.validation.ReplyOutputValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantReplyPostProcessorTest {

    @Mock
    private LlmRewriteService rewriteService;

    private final ReplyOutputValidator validator = new ReplyOutputValidator(new ObjectMapper());

    @Test
    void shouldReturnRawReplyWhenValid() {
        AssistantReplyPostProcessor processor = new AssistantReplyPostProcessor(validator, rewriteService);
        String rawReply = "行吧，这个确实可以。{\"mood\":55,\"emoji\":\"happy\"}";

        String result = processor.process("我做完了", rawReply, DialogueScene.ACHIEVEMENT_SHARE);

        assertThat(result).isEqualTo(rawReply);
        verify(rewriteService, never()).rewrite(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRewriteInvalidReply() {
        AssistantReplyPostProcessor processor = new AssistantReplyPostProcessor(validator, rewriteService);
        String rewritten = "这话本龙听着有点不舒服。你可以说哪里不对。{\"mood\":72,\"emoji\":\"angry\"}";
        when(rewriteService.rewrite(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(rewritten);

        String result = processor.process("我有点难受", "我会一直听，你可以慢慢来。{\"mood\":45,\"emoji\":\"peace\"}", DialogueScene.EMOTION_LIGHT);

        assertThat(result).isEqualTo(rewritten);
        verify(rewriteService).rewrite(
            org.mockito.ArgumentMatchers.eq("我有点难受"),
            org.mockito.ArgumentMatchers.eq("我会一直听，你可以慢慢来。{\"mood\":45,\"emoji\":\"peace\"}"),
            org.mockito.ArgumentMatchers.eq(DialogueScene.EMOTION_LIGHT),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldFallbackDirectlyOnHighRiskReply() {
        AssistantReplyPostProcessor processor = new AssistantReplyPostProcessor(validator, rewriteService);

        String result = processor.process("我今天好累", "没干活还累？这理由本龙可不信。{\"mood\":40,\"emoji\":\"speechless\"}", DialogueScene.FATIGUE);

        assertThat(result).contains("累就先别硬撑");
        verify(rewriteService, never()).rewrite(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldFallbackWhenRewriteStillInvalid() {
        AssistantReplyPostProcessor processor = new AssistantReplyPostProcessor(validator, rewriteService);
        when(rewriteService.rewrite(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn("啧，还是不行。");

        String result = processor.process("我有点难受", "我会一直听，你可以慢慢来。{\"mood\":45,\"emoji\":\"peace\"}", DialogueScene.EMOTION_LIGHT);

        assertThat(result).contains("本龙听见了");
        assertThat(result).contains("{\"mood\":45,\"emoji\":\"peace\"}");
    }
}
