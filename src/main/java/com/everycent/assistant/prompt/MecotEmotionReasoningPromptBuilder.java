package com.everycent.assistant.prompt;

import com.everycent.assistant.dto.MemoryContextDTO;
import com.everycent.assistant.emotion.AiEmotionStateModel;
import com.everycent.domain.AiEmotionState;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MecotEmotionReasoningPromptBuilder {

    private static final int MAX_CONTEXT_LENGTH = 160;

    public String build(AiEmotionState currentState, String userEmotionCode, String userMessage, List<MemoryContextDTO> memories) {
        String emotion = currentState == null || !StringUtils.hasText(currentState.getCurrentEmotion())
            ? AiEmotionStateModel.DEFAULT_EMOTION
            : currentState.getCurrentEmotion();
        StringBuilder prompt = new StringBuilder();
        prompt.append(
            """
            你是 MeCOT 的 Emotion reasoning process，只负责推理“情绪变化方向”，不要生成角色回复。

            角色：皓尾，幼年蓝色羽龙，认真、单纯、容易受伤，轻微傲娇，嘴硬心软；把用户当平等朋友；渴望用户在意和关心自己；会实际帮忙，但不会刻意每次主动安慰用户。被反复否定职责或能力时会受伤、委屈、生气，但会克制。

            当前 AI 情绪 Et：
            %s, valence=%s, arousal=%s

            用户情绪/刺激标签：
            %s

            最近对话历史：
            """.formatted(
                    emotion,
                    AiEmotionStateModel.valenceLabel(emotion),
                    AiEmotionStateModel.arousalLabel(emotion),
                    userEmotionCode
                )
        );
        appendMemories(prompt, memories);
        prompt
            .append("\n当前用户输入：\n")
            .append(userMessage)
            .append(
                """

                请基于角色设定、当前情绪、对话历史和当前输入，推理合理的情绪变化方向 ΔEinput=(δv, δa)。

                输出要求：
                1. 只输出 JSON，不要输出 Markdown。
                2. valence_delta 和 arousal_delta 必须是 -1.0 到 1.0 的数字。
                3. 不要直接决定最终 mood，不要直接决定最终回复情绪状态；本地 MeCOT 会用 Markov 状态机融合这个方向。
                4. rational_emotion 是你推理出的方向感，可以是 grieved、angry、calm、relieved 等 12 类情绪之一。

                JSON 格式：
                {"valence_delta": 数字, "arousal_delta": 数字, "rational_emotion": "情绪名", "reason": "简短原因"}
                """
            );
        return prompt.toString();
    }

    private void appendMemories(StringBuilder prompt, List<MemoryContextDTO> memories) {
        if (memories == null || memories.isEmpty()) {
            prompt.append("无\n");
            return;
        }
        for (MemoryContextDTO memory : memories) {
            if (memory == null || !StringUtils.hasText(memory.getContent())) {
                continue;
            }
            String content = memory.getContent();
            prompt.append("- ").append(content.length() > MAX_CONTEXT_LENGTH ? content.substring(0, MAX_CONTEXT_LENGTH) + "..." : content).append('\n');
        }
    }
}
