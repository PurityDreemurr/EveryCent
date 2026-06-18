package com.everycent.assistant.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.assistant.emotion.AiEmotionPromptAdapter;
import com.everycent.assistant.emotion.AiEmotionStateModel;
import com.everycent.assistant.emotion.AiEmotionTransitionResult;
import com.everycent.assistant.emotion.MecotEmotionReasoningResponseParser;
import com.everycent.assistant.emotion.MecotEmotionService;
import com.everycent.assistant.emotion.MecotRationalEmotionVector;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.enumeration.EmotionValence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssistantPromptBuilderMecotStyleTest {

    @Test
    void shouldIgnoreMecotAndMemoryContextWhenPurePromptModeIsEnabled() {
        MecotEmotionService service = new MecotEmotionService();
        AiEmotionTransitionResult grieved = service.transition(
            AiEmotionStateModel.state("calm"),
            emotionTag("INVALIDATED"),
            "你记什么账啊，我又没让你记。",
            List.of(),
            new MecotRationalEmotionVector(-1.0, 0.78, "grieved", "职责被否定，受伤委屈")
        );
        AiEmotionTransitionResult angry = service.transition(
            AiEmotionStateModel.state(grieved.getAfterEmotion()),
            emotionTag("INVALIDATED"),
            "我看你根本就不懂记账，记了也没用。",
            List.of(),
            new MecotRationalEmotionVector(0.18, 0.27, "angry", "连续否定能力，转向克制愤怒")
        );

        String style = new AiEmotionPromptAdapter().buildStyleInstruction(AiEmotionStateModel.state(grieved.getAfterEmotion()), angry);
        String prompt = new AssistantPromptBuilder()
            .buildSingleTurnPrompt("算了，你这个记账功能就是摆设。", "中立", angry, style, List.of());

        assertThat(angry.getAfterEmotion()).isEqualTo("angry");
        assertThat(prompt).doesNotContain("[AI 当前 MeCOT 状态]");
        assertThat(prompt).doesNotContain("可以明显表现出生气");
        assertThat(prompt).doesNotContain("mood 建议 80-90");
        assertThat(prompt).doesNotContain("不要压成平静客服腔");
        assertThat(prompt).doesNotContain("[长期记忆 Top-K]");
    }

    @Test
    void shouldBuildAndParseEmotionReasoningVectorPrompt() {
        String prompt = new MecotEmotionReasoningPromptBuilder()
            .build(AiEmotionStateModel.state("grieved"), "INVALIDATED", "我看你根本就不懂记账，记了也没用。", List.of());
        assertThat(prompt).contains("只负责推理“情绪变化方向”");
        assertThat(prompt).contains("valence_delta");
        assertThat(prompt).contains("arousal_delta");
        assertThat(prompt).contains("不要直接决定最终 mood");

        MecotRationalEmotionVector vector = new MecotEmotionReasoningResponseParser(new ObjectMapper())
            .parse("{\"valence_delta\":0.18,\"arousal_delta\":0.27,\"rational_emotion\":\"angry\",\"reason\":\"被否定能力\"}");
        assertThat(vector.getValenceDelta()).isEqualTo(0.18);
        assertThat(vector.getArousalDelta()).isEqualTo(0.27);
        assertThat(vector.getRationalEmotion()).isEqualTo("angry");
    }

    @Test
    void shouldBuildFunctionalEveryCentAssistantPrompt() {
        String prompt = new AssistantPromptBuilder().buildSingleTurnPrompt("嗯。", "中立", List.of());

        assertThat(prompt).contains("你是 EveryCent 财务助手");
        assertThat(prompt).contains("功能型记账与预算辅助 AI");
        assertThat(prompt).contains("优先帮助用户记录账单、识别收支、整理备注、提醒预算风险");
        assertThat(prompt).contains("不要角色扮演，不要拟人化表演");
        assertThat(prompt).contains("当用户输入包含明确账单信息时，优先处理记账");
        assertThat(prompt).contains("如果用户只是日常聊天或抱怨，且没有金额或明确记账意图，不要主动记账");
        assertThat(prompt).contains("不要出现“皓尾”“本龙”“龙”“龙宫”“翅膀”“尾巴”等角色扮演内容");
        assertThat(prompt).contains("{\"mood\": 数字, \"emoji\": \"枚举值\"}");
        assertThat(prompt).doesNotContain("皓尾活泼、认真、嘴硬心软");
        assertThat(prompt).doesNotContain("幼年蓝色羽龙");
        assertThat(prompt).doesNotContain("皓尾可以自称");
        assertThat(prompt).doesNotContain("每次回复最多使用一个龙族元素");
    }

    private EmotionTag emotionTag(String code) {
        EmotionTag tag = new EmotionTag();
        tag.setCode(code);
        tag.setName(code);
        tag.setValence(EmotionValence.NEGATIVE);
        tag.setSystemDefault(true);
        return tag;
    }
}
