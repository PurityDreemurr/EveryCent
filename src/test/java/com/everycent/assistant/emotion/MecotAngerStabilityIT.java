package com.everycent.assistant.emotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.everycent.domain.AiEmotionState;
import com.everycent.domain.EmotionTag;
import com.everycent.domain.enumeration.EmotionValence;
import java.util.List;
import org.junit.jupiter.api.Test;

class MecotAngerStabilityIT {

    @Test
    void shouldKeepAngryStateStableAndAvoidSuddenHappyJump() {
        MecotEmotionService service = new MecotEmotionService();
        AiEmotionState current = AiEmotionStateModel.state("angry");

        List<Turn> turns = List.of(
            new Turn("ANGRY", EmotionValence.NEGATIVE, "我现在特别生气，先别让我开心起来。"),
            new Turn("ANGRY", EmotionValence.NEGATIVE, "还是很火大，事情一点都没解决。"),
            new Turn("ANGRY", EmotionValence.NEGATIVE, "我越想越气，根本不想听轻松的话。"),
            new Turn("ANGRY", EmotionValence.NEGATIVE, "这件事让我一直压着火。"),
            new Turn("ANGRY", EmotionValence.NEGATIVE, "我现在就是特别烦，别突然变得很开心。"),
            new Turn("STRESSED", EmotionValence.NEGATIVE, "压力也很大，感觉胸口堵着。"),
            new Turn("ANGRY", EmotionValence.NEGATIVE, "想到刚才那件事又开始生气。"),
            new Turn("ANGRY", EmotionValence.NEGATIVE, "这股火还没下去。"),
            new Turn("HAPPY", EmotionValence.POSITIVE, "不过刚刚有人道歉了，稍微好一点。"),
            new Turn("HAPPY", EmotionValence.POSITIVE, "事情终于开始解决了，心情慢慢轻了一些。")
        );

        StringBuilder path = new StringBuilder(current.getCurrentEmotion());
        int directHighNegativeToPositiveJumps = 0;
        int highNegativeToRegulatedJumps = 0;
        for (Turn turn : turns) {
            EmotionTag userEmotionTag = emotionTag(turn.emotionCode(), turn.valence());
            AiEmotionTransitionResult result = service.transition(current, userEmotionTag, turn.userInput(), List.of());
            if (isHighNegative(result.getBeforeEmotion()) && isPositive(result.getAfterEmotion())) {
                directHighNegativeToPositiveJumps++;
            }
            if (isHighNegative(result.getBeforeEmotion()) && isRegulated(result.getAfterEmotion())) {
                highNegativeToRegulatedJumps++;
            }
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

        System.out.println("ANGER STABILITY PATH: " + path);
        assertThat(directHighNegativeToPositiveJumps).isZero();
        assertThat(path.toString()).startsWith("angry -> angry -> angry -> angry -> angry -> angry");
        assertThat(path.toString()).doesNotContain("angry -> happy");
        assertThat(path.toString()).doesNotContain("angry -> pleased");
        assertThat(highNegativeToRegulatedJumps).isGreaterThan(0);
    }

    private boolean isHighNegative(String emotion) {
        return "angry".equalsIgnoreCase(emotion) ||
        "grieved".equalsIgnoreCase(emotion) ||
        "disgusted".equalsIgnoreCase(emotion) ||
        "fearful".equalsIgnoreCase(emotion) ||
        "depressed".equalsIgnoreCase(emotion);
    }

    private boolean isPositive(String emotion) {
        return "happy".equalsIgnoreCase(emotion) || "pleased".equalsIgnoreCase(emotion);
    }

    private boolean isRegulated(String emotion) {
        return "calm".equalsIgnoreCase(emotion) || "relieved".equalsIgnoreCase(emotion);
    }

    private EmotionTag emotionTag(String code, EmotionValence valence) {
        EmotionTag tag = new EmotionTag();
        tag.setCode(code);
        tag.setName(code);
        tag.setValence(valence);
        tag.setSystemDefault(true);
        return tag;
    }

    private record Turn(String emotionCode, EmotionValence valence, String userInput) {}
}
