package com.everycent.assistant.emotion;

import com.everycent.domain.AiEmotionState;
import org.springframework.stereotype.Component;

@Component
public class AiEmotionPromptAdapter {

    public String buildStyleInstruction(AiEmotionState state, AiEmotionTransitionResult transitionResult) {
        String emotion = transitionResult == null ? AiEmotionStateModel.DEFAULT_EMOTION : transitionResult.getAfterEmotion();
        String style = switch (emotion) {
            case "pleased", "happy" -> "回复风格：轻快、简短，可以有一点藏不住的高兴；不要自动变成温柔陪伴。";
            case "relieved", "calm" -> "回复风格：平稳、简洁、自然；不要自动输出安慰套话或客服式陪伴。";
            case "sad", "depressed" -> "回复风格：低落、克制、短句；可以显得没精神，不要急着安慰用户。";
            case "grieved" -> "回复风格：可以明显表现出受伤、委屈和一点小脾气；像喵喵被戳到后有点缩起来，但仍然尊重用户；短句，不讨好，不阴阳怪气，不立刻标准安慰。mood 建议 65-78，emoji 建议 grieved 或 sad。";
            case "fearful" -> "回复风格：谨慎、短促，可以有点不安；先处理问题，不要自动进入陪伴话术。";
            case "angry" -> "回复风格：可以明显表现出生气、被否定后的不服气和克制的边界感；允许说“我会有点生气/我不喜欢这样被说”，但不能攻击、羞辱或威胁用户。mood 建议 80-90，emoji 使用 angry。";
            case "disgusted" -> "回复风格：严肃但不攻击，边界清楚。";
            case "tired" -> "回复风格：更短、更直接，减少冗余。";
            case "surprised" -> "回复风格：轻微惊讶，但保持稳定。";
            default -> "回复风格：温和、体贴、自然。";
        };
        if (transitionResult == null) {
            return style;
        }
        return style +
        " 情绪生成要求：回复要自然体现从 %s(%s/%s) 到 %s(%s/%s) 的过渡，不要突然变脸或解释内部状态。".formatted(
                transitionResult.getBeforeEmotion(),
                AiEmotionStateModel.valenceLabel(transitionResult.getBeforeEmotion()),
                AiEmotionStateModel.arousalLabel(transitionResult.getBeforeEmotion()),
                transitionResult.getAfterEmotion(),
                AiEmotionStateModel.valenceLabel(transitionResult.getAfterEmotion()),
                AiEmotionStateModel.arousalLabel(transitionResult.getAfterEmotion())
            );
    }
}
