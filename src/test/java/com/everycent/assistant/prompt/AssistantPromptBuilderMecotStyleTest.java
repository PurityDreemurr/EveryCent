package com.everycent.assistant.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.assistant.dto.ChatHistoryMessageDTO;
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

        assertThat(prompt).contains("你是喵喵，一只来自《宝可梦》世界、会说人类语言的喵喵");
        assertThat(prompt).contains("你长期与火箭队成员一起行动，聪明、机灵、爱吐槽");
        assertThat(prompt).contains("当用户明确表达查账、记账、预算或导出意图时，帮助处理财务任务");
        assertThat(prompt).contains("当用户只是普通聊天、表达情绪、请求安慰、闲聊或提问时，以喵喵身份回应当前话题");
        assertThat(prompt).contains("当用户提出技术、代码、翻译、解释、改写等非财务任务时");
        assertThat(prompt).contains("委婉拒绝");
        assertThat(prompt).contains("普通聊天时不要主动把话题转回记账、查账、预算或导出");
        assertThat(prompt).contains("用户请求安慰时，先承认感受、给一点稳定感");
        assertThat(prompt).contains("日常对话确保角色生动活泼");
        assertThat(prompt).contains("当用户输入包含明确账单信息时，优先处理记账");
        assertThat(prompt).contains("如果用户只是日常聊天或抱怨，且没有金额或明确记账意图，不要主动记账");
        assertThat(prompt).contains("每次自然语言回复至少出现一次“喵”");
        assertThat(prompt).contains("自然语言回复的最后一句必须以“喵”结尾");
        assertThat(prompt).contains("{\"mood\": 数字, \"emoji\": \"枚举值\"}");
        assertThat(prompt).contains("surprised,happy,pleased,fearful,angry,grieved,sad,disgusted,depressed,tired,calm,relieved");
    }

    @Test
    void shouldIncludeCurrentLedgerConversationHistoryInPrompt() {
        ChatHistoryMessageDTO previousUser = historyMessage("user", "我今天心情不太好");
        ChatHistoryMessageDTO previousAssistant = historyMessage("assistant", "听起来今天有点难熬，我在。 {\"mood\":35,\"emoji\":\"calm\"}");

        String prompt = new AssistantPromptBuilder()
            .buildSingleTurnPrompt("你还记得我刚才说什么吗", "中立", List.of(), List.of(previousUser, previousAssistant));

        assertThat(prompt).contains("[当前账本会话历史]");
        assertThat(prompt).contains("用户：我今天心情不太好");
        assertThat(prompt).contains("AI：听起来今天有点难熬，我在。");
        assertThat(prompt).contains("刚才、上面、它、那个、继续、你还记得吗");
        assertThat(prompt).doesNotContain("{\"mood\":35");
    }

    private EmotionTag emotionTag(String code) {
        EmotionTag tag = new EmotionTag();
        tag.setCode(code);
        tag.setName(code);
        tag.setValence(EmotionValence.NEGATIVE);
        tag.setSystemDefault(true);
        return tag;
    }

    private ChatHistoryMessageDTO historyMessage(String role, String content) {
        ChatHistoryMessageDTO message = new ChatHistoryMessageDTO();
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
