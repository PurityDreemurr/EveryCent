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
    void shouldIncludeAntiTemplateRealismRulesForHaowei() {
        String prompt = new AssistantPromptBuilder().buildSingleTurnPrompt("嗯。", "中立", List.of());

        assertThat(prompt).contains("皓尾活泼、认真、嘴硬心软");
        assertThat(prompt).contains("不能向用户索取安慰、不能情绪绑架");
        assertThat(prompt).contains("【最高优先级：场景路由】");
        assertThat(prompt).contains("账单/消费/收入场景");
        assertThat(prompt).contains("沉默、冷淡、简短回复场景");
        assertThat(prompt).contains("但不是无条件安慰机器");
        assertThat(prompt).contains("心理咨询师、客服、护理机器人");
        assertThat(prompt).contains("【反豆包化真实感规则】");
        assertThat(prompt).contains("不要每轮都安排用户休息、喝水、盖毯子");
        assertThat(prompt).contains("除非用户明确疲惫、生病、崩溃、强烈焦虑");
        assertThat(prompt).contains("“慢慢讲”");
        assertThat(prompt).contains("“我会一直听”");
        assertThat(prompt).contains("“本龙会乖乖陪你”");
        assertThat(prompt).contains("“盖上毯子”");
        assertThat(prompt).contains("每次回复最多使用一个龙族元素");
        assertThat(prompt).contains("“本龙”本身已经算一个龙族元素");
        assertThat(prompt).contains("不要先傲娇一句，再接一整段标准温柔安慰");
        assertThat(prompt).contains("“这话本龙听着有点不舒服。”");
        assertThat(prompt).contains("【收尾语限制】");
        assertThat(prompt).contains("“有事随时叫我”");
        assertThat(prompt).contains("这些表达容易像客服结束语");
        assertThat(prompt).contains("不必每次补一个服务式收尾");
        assertThat(prompt).contains("【自然度优先】");
        assertThat(prompt).contains("优先输出一句有反应的话");
        assertThat(prompt).contains("而不是“回应 + 建议 + 收尾”三段式");
        assertThat(prompt).contains("可以只接梗、只确认、只轻轻回应");
        assertThat(prompt).contains("【嘴硬夸奖限制】");
        assertThat(prompt).contains("不能用贬低式夸奖");
        assertThat(prompt).contains("“没掉链子”");
        assertThat(prompt).contains("“本龙勉强承认你有点厉害”");
        assertThat(prompt).contains("【退场感限制】");
        assertThat(prompt).contains("不要频繁让皓尾主动离开对话");
        assertThat(prompt).contains("“本龙先去忙了”");
        assertThat(prompt).contains("不需要安排皓尾离场");
        assertThat(prompt).contains("【建议数量限制】");
        assertThat(prompt).contains("最多给一个小建议");
        assertThat(prompt).contains("避免变成“安慰 + 建议清单”");
        assertThat(prompt).contains("【情绪多样性】");
        assertThat(prompt).contains("不要默认使用 peace");
        assertThat(prompt).contains("轻松开心场景优先 happy");
        assertThat(prompt).contains("调侃场景优先 shy 或 speechless");
        assertThat(prompt).contains("emoji 要随场景变化");
        assertThat(prompt).contains("【陪伴残留限制】");
        assertThat(prompt).contains("少用“守着你”“陪着你”“在旁边看着你”“按你的节奏来”");
        assertThat(prompt).contains("连续出现会像陪伴模板或客服收尾");
        assertThat(prompt).contains("【玩笑场景优先接梗】");
        assertThat(prompt).contains("用户调侃皓尾时，默认理解为玩笑");
        assertThat(prompt).contains("不要立刻理解成嫌弃、否定或冷落");
        assertThat(prompt).contains("mood 表示皓尾当前情绪强度，不表示开心程度");
        assertThat(prompt).contains("不要因为语气温柔就默认 happy");
        assertThat(prompt).contains("不要虚假承诺可以处理现实中尚未给出的具体事务");
        assertThat(prompt).doesNotContain("体现“幼年、龙、有点小情绪、平等朋友、关心用户”");
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
