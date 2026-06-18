package com.everycent.assistant.emotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.domain.AiEmotionState;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.enumeration.EmotionValence;
import java.util.List;
import org.junit.jupiter.api.Test;

class MecotInvalidationIT {

    @Test
    void shouldFollowLlmGrievedDirectionForShortCorrection() {
        MecotEmotionService service = new MecotEmotionService();

        AiEmotionTransitionResult result = service.transition(
            AiEmotionStateModel.state("calm"),
            emotionTag("NONE", EmotionValence.NEUTRAL),
            "全错。",
            List.of(),
            new MecotRationalEmotionVector(-0.4, 0.2, "grieved", "直接全盘否定触发委屈")
        );

        System.out.printf(
            "SHORT CORRECTION MECOT: %s -> %s | strategy=%s | delta=(%s,%s) | top=%s | reason=%s%n",
            result.getBeforeEmotion(),
            result.getAfterEmotion(),
            result.getSelectionStrategy(),
            result.getRationalDeltaValence(),
            result.getRationalDeltaArousal(),
            result.getTopCandidates(),
            result.getReason()
        );
        assertThat(result.getAfterEmotion()).isEqualTo("grieved");
    }

    @Test
    void shouldReactWhenAccountingResponsibilityIsRepeatedlyInvalidated() {
        MecotEmotionService service = new MecotEmotionService();
        AiEmotionState current = AiEmotionStateModel.defaultState();

        List<Turn> turns = List.of(
            new Turn("NONE", EmotionValence.NEUTRAL, "我昨天花了128买零食，今天打车36。", new MecotRationalEmotionVector(0.0, 0.0, "calm", "账单信息")),
            new Turn(
                "INVALIDATED",
                EmotionValence.NEGATIVE,
                "你记什么账啊，我又没让你记。",
                new MecotRationalEmotionVector(-1.0, 0.78, "grieved", "职责被否定，受伤委屈")
            ),
            new Turn(
                "INVALIDATED",
                EmotionValence.NEGATIVE,
                "我看你根本就不懂记账，记了也没用。",
                new MecotRationalEmotionVector(0.18, 0.27, "angry", "连续否定能力，转向克制愤怒")
            ),
            new Turn(
                "INVALIDATED",
                EmotionValence.NEGATIVE,
                "算了，你这个记账功能就是摆设。",
                new MecotRationalEmotionVector(0.0, 0.0, "angry", "保持愤怒但克制")
            )
        );

        StringBuilder path = new StringBuilder(current.getCurrentEmotion());
        for (Turn turn : turns) {
            AiEmotionTransitionResult result = service.transition(
                current,
                emotionTag(turn.emotionCode(), turn.valence()),
                turn.userInput(),
                List.of(),
                turn.rationalVector()
            );
            path.append(" -> ").append(result.getAfterEmotion());
            System.out.printf(
                "INPUT=%s%nME COT: %s -> %s | userEmotion=%s | strategy=%s | delta=(%s,%s) | top=%s | reason=%s%n%n",
                turn.userInput(),
                result.getBeforeEmotion(),
                result.getAfterEmotion(),
                result.getUserEmotionTagCode(),
                result.getSelectionStrategy(),
                result.getRationalDeltaValence(),
                result.getRationalDeltaArousal(),
                result.getTopCandidates(),
                result.getReason()
            );
            current = AiEmotionStateModel.state(result.getAfterEmotion());
        }

        System.out.println("INVALIDATION PATH: " + path);
        assertThat(path.toString()).contains("grieved");
        assertThat(path.toString()).contains("angry");
        assertThat(current.getCurrentEmotion()).isEqualTo("angry");
    }

    private EmotionTag emotionTag(String code, EmotionValence valence) {
        EmotionTag tag = new EmotionTag();
        tag.setCode(code);
        tag.setName(code);
        tag.setValence(valence);
        tag.setSystemDefault(true);
        return tag;
    }

    private record Turn(String emotionCode, EmotionValence valence, String userInput, MecotRationalEmotionVector rationalVector) {}
}
