package com.everycent.assistant.emotion;

import com.everycent.domain.AiEmotionState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiEmotionStateModel {

    public static final String DEFAULT_EMOTION = "calm";

    private static final Map<String, Point> STATES = new LinkedHashMap<>();

    static {
        STATES.put("surprised", new Point(0.383, 0.924));
        STATES.put("happy", new Point(0.707, 0.707));
        STATES.put("pleased", new Point(0.924, 0.383));
        STATES.put("fearful", new Point(-0.383, 0.924));
        STATES.put("angry", new Point(-0.707, 0.707));
        STATES.put("grieved", new Point(-0.924, 0.383));
        STATES.put("sad", new Point(-0.924, -0.383));
        STATES.put("disgusted", new Point(-0.707, -0.707));
        STATES.put("depressed", new Point(-0.383, -0.924));
        STATES.put("tired", new Point(0.383, -0.924));
        STATES.put("calm", new Point(0.707, -0.707));
        STATES.put("relieved", new Point(0.924, -0.383));
    }

    private AiEmotionStateModel() {}

    public static AiEmotionState defaultState() {
        return state(DEFAULT_EMOTION);
    }

    public static AiEmotionState state(String emotion) {
        Point point = point(emotion);
        AiEmotionState state = new AiEmotionState();
        state.setCurrentEmotion(emotion);
        state.setValence(scale(point.valence()));
        state.setArousal(scale(point.arousal()));
        return state;
    }

    public static Point point(String emotion) {
        return STATES.getOrDefault(emotion, STATES.get(DEFAULT_EMOTION));
    }

    public static Map<String, Point> states() {
        return Map.copyOf(STATES);
    }

    public static String valenceLabel(String emotion) {
        double valence = point(emotion).valence();
        if (valence > 0.2) {
            return "positive";
        }
        if (valence < -0.2) {
            return "negative";
        }
        return "neutral";
    }

    public static String arousalLabel(String emotion) {
        double arousal = point(emotion).arousal();
        if (arousal > 0.45) {
            return "high";
        }
        if (arousal < -0.45) {
            return "low";
        }
        return "medium";
    }

    public static String nearest(double valence, double arousal) {
        String best = DEFAULT_EMOTION;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, Point> entry : STATES.entrySet()) {
            Point point = entry.getValue();
            double distance = Math.pow(point.valence() - valence, 2) + Math.pow(point.arousal() - arousal, 2);
            if (distance < bestDistance) {
                best = entry.getKey();
                bestDistance = distance;
            }
        }
        return best;
    }

    public static BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    public record Point(double valence, double arousal) {}
}
