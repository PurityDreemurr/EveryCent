package com.everycent.assistant.emotion;

import com.everycent.assistant.dto.EmotionTransitionDTO;
import com.everycent.domain.AiEmotionState;
import com.everycent.domain.EmotionTag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MecotEmotionService {

    private static final double SLOW_PROCESS_WEIGHT = 0.85;
    private static final double INVALIDATION_SLOW_PROCESS_WEIGHT = 1.60;
    private static final double RATIONAL_EMOTION_PULL_WEIGHT = 1.25;
    private static final double MIN_THRESHOLD = 0.04;
    private static final int TOP_CANDIDATE_LIMIT = 5;

    private static final Map<String, Double> HAOWEI_PERSONALITY_WEIGHTS = Map.ofEntries(
        Map.entry("surprised", 0.86),
        Map.entry("happy", 0.50),
        Map.entry("pleased", 0.58),
        Map.entry("fearful", 0.62),
        Map.entry("angry", 0.70),
        Map.entry("grieved", 0.92),
        Map.entry("sad", 0.76),
        Map.entry("disgusted", 0.20),
        Map.entry("depressed", 0.32),
        Map.entry("tired", 0.56),
        Map.entry("calm", 0.54),
        Map.entry("relieved", 0.58)
    );

    public AiEmotionTransitionResult transition(
        AiEmotionState currentState,
        EmotionTag userEmotionTag,
        String userMessage,
        List<?> memories
    ) {
        AiEmotionState before = normalize(currentState);
        String userEmotionCode = userEmotionTag == null || !StringUtils.hasText(userEmotionTag.getCode())
            ? "NONE"
            : userEmotionTag.getCode().toUpperCase(Locale.ROOT);
        MecotRationalEmotionVector rationalVector = inferRationalEmotionVector(before.getCurrentEmotion(), userEmotionCode, userMessage, memories);
        return transition(currentState, userEmotionTag, userMessage, memories, rationalVector);
    }

    public AiEmotionTransitionResult transition(
        AiEmotionState currentState,
        EmotionTag userEmotionTag,
        String userMessage,
        List<?> memories,
        MecotRationalEmotionVector rationalVector
    ) {
        AiEmotionState before = normalize(currentState);
        String userEmotionCode = userEmotionTag == null || !StringUtils.hasText(userEmotionTag.getCode())
            ? "NONE"
            : userEmotionTag.getCode().toUpperCase(Locale.ROOT);
        MecotRationalEmotionVector resolvedVector = rationalVector == null
            ? new MecotRationalEmotionVector(0.0, 0.0, "calm", "missing_llm_vector")
            : rationalVector;
        Map<String, Double> distribution = transitionDistribution(before.getCurrentEmotion(), resolvedVector, userEmotionCode);
        MecotSelectionStrategy strategy = chooseSelectionStrategy(userEmotionCode, userMessage, resolvedVector);
        String targetEmotion = selectEmotion(distribution, strategy, before.getCurrentEmotion(), resolvedVector);
        AiEmotionState after = AiEmotionStateModel.state(targetEmotion);

        AiEmotionTransitionResult result = new AiEmotionTransitionResult();
        result.setBeforeEmotion(before.getCurrentEmotion());
        result.setAfterEmotion(after.getCurrentEmotion());
        result.setBeforeValence(before.getValence());
        result.setBeforeArousal(before.getArousal());
        result.setAfterValence(after.getValence());
        result.setAfterArousal(after.getArousal());
        result.setUserEmotionTagCode(userEmotionCode);
        result.setSelectionStrategy(strategy);
        result.setRationalDeltaValence(scale(resolvedVector.getValenceDelta()));
        result.setRationalDeltaArousal(scale(resolvedVector.getArousalDelta()));
        result.setPersonalityProfile("haowei:easily-hurt, naive, lightly-tsundere, wants-user-care, action-oriented-help");
        result.setTopCandidates(topCandidates(distribution));
        result.setReason(buildReason(userEmotionCode, userMessage, memories, before.getCurrentEmotion(), after.getCurrentEmotion(), strategy, resolvedVector));
        return result;
    }

    public EmotionTransitionDTO toDto(AiEmotionTransitionResult result) {
        EmotionTransitionDTO dto = new EmotionTransitionDTO();
        dto.setBeforeEmotion(result.getBeforeEmotion());
        dto.setAfterEmotion(result.getAfterEmotion());
        dto.setBeforeValence(result.getBeforeValence());
        dto.setBeforeArousal(result.getBeforeArousal());
        dto.setAfterValence(result.getAfterValence());
        dto.setAfterArousal(result.getAfterArousal());
        dto.setReason(result.getReason());
        return dto;
    }

    private AiEmotionState normalize(AiEmotionState state) {
        if (state != null && StringUtils.hasText(state.getCurrentEmotion())) {
            return state;
        }
        return AiEmotionStateModel.defaultState();
    }

    private Map<String, Double> transitionDistribution(String beforeEmotion, MecotRationalEmotionVector rationalVector, String userEmotionCode) {
        Map<String, Double> raw = new LinkedHashMap<>();
        AiEmotionStateModel.Point before = AiEmotionStateModel.point(beforeEmotion);
        double slowProcessWeight = "INVALIDATED".equals(userEmotionCode) ? INVALIDATION_SLOW_PROCESS_WEIGHT : SLOW_PROCESS_WEIGHT;
        for (Map.Entry<String, AiEmotionStateModel.Point> entry : AiEmotionStateModel.states().entrySet()) {
            String target = entry.getKey();
            AiEmotionStateModel.Point point = entry.getValue();
            double base = baseTransitionProbability(before, point);
            double slowAlignment = Math.max(0.0, rationalVector.getValenceDelta() * point.valence() + rationalVector.getArousalDelta() * point.arousal());
            double rationalPull = rationalEmotionPull(target, rationalVector);
            double personality = HAOWEI_PERSONALITY_WEIGHTS.getOrDefault(target, 0.5);
            raw.put(target, base + (slowProcessWeight * slowAlignment + rationalPull) * personality);
        }
        return normalize(raw);
    }

    private double rationalEmotionPull(String target, MecotRationalEmotionVector rationalVector) {
        if (rationalVector == null || !StringUtils.hasText(rationalVector.getRationalEmotion())) {
            return 0.0;
        }
        String rationalEmotion = rationalVector.getRationalEmotion().toLowerCase(Locale.ROOT);
        if (!AiEmotionStateModel.states().containsKey(rationalEmotion)) {
            return 0.0;
        }
        double intensity = Math.min(1.0, Math.hypot(rationalVector.getValenceDelta(), rationalVector.getArousalDelta()));
        if (intensity < 0.25) {
            return 0.0;
        }
        AiEmotionStateModel.Point targetPoint = AiEmotionStateModel.point(target);
        AiEmotionStateModel.Point rationalPoint = AiEmotionStateModel.point(rationalEmotion);
        double distanceSquared = Math.pow(targetPoint.valence() - rationalPoint.valence(), 2) + Math.pow(targetPoint.arousal() - rationalPoint.arousal(), 2);
        return RATIONAL_EMOTION_PULL_WEIGHT * intensity * Math.exp(-distanceSquared);
    }

    private double baseTransitionProbability(AiEmotionStateModel.Point before, AiEmotionStateModel.Point target) {
        double distanceSquared = Math.pow(before.valence() - target.valence(), 2) + Math.pow(before.arousal() - target.arousal(), 2);
        return Math.exp(-distanceSquared);
    }

    private Map<String, Double> normalize(Map<String, Double> raw) {
        double sum = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> normalized = new LinkedHashMap<>();
        if (sum <= 0.0) {
            double fallback = 1.0 / raw.size();
            raw.keySet().forEach(key -> normalized.put(key, fallback));
            return normalized;
        }
        raw.forEach((emotion, probability) -> normalized.put(emotion, probability / sum));
        return normalized;
    }

    private MecotRationalEmotionVector inferRationalEmotionVector(String beforeEmotion, String userEmotionCode, String userMessage, List<?> memories) {
        AiEmotionStateModel.Point before = AiEmotionStateModel.point(beforeEmotion);
        AiEmotionStateModel.Point target = targetPoint(userEmotionCode, userMessage);
        double inertia = highNegative(beforeEmotion) ? 0.68 : 0.42;
        if ("ANGRY".equals(userEmotionCode)) {
            inertia = 0.78;
        }
        if ("INVALIDATED".equals(userEmotionCode)) {
            target = invalidationTarget(beforeEmotion, userMessage);
            inertia = switch (beforeEmotion == null ? "" : beforeEmotion.toLowerCase(Locale.ROOT)) {
                case "calm", "relieved", "pleased", "happy" -> 0.28;
                case "grieved", "sad" -> 0.18;
                case "angry" -> 0.55;
                default -> 0.35;
            };
        }
        if ("HAPPY".equals(userEmotionCode) && highNegative(beforeEmotion)) {
            target = AiEmotionStateModel.point("relieved");
            inertia = 0.25;
        }
        double memoryAdjustment = memories == null || memories.isEmpty() ? 0.0 : 0.04;
        String rationalEmotion = AiEmotionStateModel.nearest(target.valence(), target.arousal());
        return new MecotRationalEmotionVector(
            clamp((target.valence() - before.valence()) * (1.0 - inertia) + memoryAdjustment, -1.0, 1.0),
            clamp((target.arousal() - before.arousal()) * (1.0 - inertia), -1.0, 1.0),
            rationalEmotion,
            "local_fallback"
        );
    }

    private AiEmotionStateModel.Point targetPoint(String userEmotionCode, String userMessage) {
        if ("ANGRY".equals(userEmotionCode)) {
            return AiEmotionStateModel.point("angry");
        }
        if ("INVALIDATED".equals(userEmotionCode)) {
            return invalidationTarget(null, userMessage);
        }
        if ("HAPPY".equals(userEmotionCode)) {
            return AiEmotionStateModel.point("pleased");
        }
        if ("CALM".equals(userEmotionCode)) {
            return AiEmotionStateModel.point("calm");
        }
        if ("ANXIOUS".equals(userEmotionCode) || "IMPULSIVE".equals(userEmotionCode)) {
            return AiEmotionStateModel.point("fearful");
        }
        if ("REGRET".equals(userEmotionCode)) {
            return AiEmotionStateModel.point("sad");
        }
        if ("STRESSED".equals(userEmotionCode)) {
            return containsFinancialStress(userMessage) ? AiEmotionStateModel.point("calm") : AiEmotionStateModel.point("grieved");
        }
        if (containsFinancialStress(userMessage)) {
            return AiEmotionStateModel.point("calm");
        }
        if (containsPositiveHint(userMessage)) {
            return AiEmotionStateModel.point("relieved");
        }
        return AiEmotionStateModel.point("calm");
    }

    private MecotSelectionStrategy chooseSelectionStrategy(String userEmotionCode, String userMessage, MecotRationalEmotionVector rationalVector) {
        if ("INVALIDATED".equals(userEmotionCode)) {
            return MecotSelectionStrategy.EXPECTED_VALUE;
        }
        if ("ANGRY".equals(userEmotionCode) || "HAPPY".equals(userEmotionCode)) {
            return MecotSelectionStrategy.MAXIMUM_PROBABILITY;
        }
        if (isNegativeVectorDominant(rationalVector)) {
            return MecotSelectionStrategy.EXPECTED_VALUE;
        }
        if (containsFinancialStress(userMessage)) {
            return MecotSelectionStrategy.EXPECTED_VALUE;
        }
        return MecotSelectionStrategy.PROBABILISTIC_THRESHOLD;
    }

    private boolean isNegativeVectorDominant(MecotRationalEmotionVector rationalVector) {
        if (rationalVector == null) {
            return false;
        }
        String rationalEmotion = rationalVector.getRationalEmotion() == null ? "" : rationalVector.getRationalEmotion().toLowerCase(Locale.ROOT);
        return rationalVector.getValenceDelta() <= -0.25 && (
            "grieved".equals(rationalEmotion) ||
            "angry".equals(rationalEmotion) ||
            "sad".equals(rationalEmotion) ||
            "depressed".equals(rationalEmotion) ||
            "disgusted".equals(rationalEmotion)
        );
    }

    private String selectEmotion(Map<String, Double> distribution, MecotSelectionStrategy strategy) {
        return switch (strategy) {
            case EXPECTED_VALUE -> expectedValueEmotion(distribution);
            case MAXIMUM_PROBABILITY -> maxProbabilityEmotion(distribution);
            case PROBABILISTIC_THRESHOLD -> thresholdEmotion(distribution);
        };
    }

    private String expectedValueEmotion(Map<String, Double> distribution) {
        double valence = 0.0;
        double arousal = 0.0;
        for (Map.Entry<String, Double> entry : distribution.entrySet()) {
            AiEmotionStateModel.Point point = AiEmotionStateModel.point(entry.getKey());
            valence += entry.getValue() * point.valence();
            arousal += entry.getValue() * point.arousal();
        }
        return AiEmotionStateModel.nearest(valence, arousal);
    }

    private String maxProbabilityEmotion(Map<String, Double> distribution) {
        return distribution
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(AiEmotionStateModel.DEFAULT_EMOTION);
    }

    private String thresholdEmotion(Map<String, Double> distribution) {
        return distribution
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() >= MIN_THRESHOLD)
            .max(Comparator.comparingDouble(entry -> entry.getValue() * HAOWEI_PERSONALITY_WEIGHTS.getOrDefault(entry.getKey(), 0.5)))
            .map(Map.Entry::getKey)
            .orElseGet(() -> maxProbabilityEmotion(distribution));
    }

    private String selectEmotion(Map<String, Double> distribution, MecotSelectionStrategy strategy, String beforeEmotion, MecotRationalEmotionVector rationalVector) {
        if (shouldUseRationalDirectionGate(beforeEmotion, rationalVector)) {
            return gatedRationalEmotion(distribution, beforeEmotion, rationalVector);
        }
        String selectedEmotion = selectEmotion(distribution, strategy);
        if (highNegative(beforeEmotion) && isPositiveEmotion(selectedEmotion)) {
            return regulatedEmotion(distribution);
        }
        return selectedEmotion;
    }

    private String regulatedEmotion(Map<String, Double> distribution) {
        return distribution
            .entrySet()
            .stream()
            .filter(entry -> "calm".equals(entry.getKey()) || "relieved".equals(entry.getKey()))
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(AiEmotionStateModel.DEFAULT_EMOTION);
    }

    private boolean isPositiveEmotion(String emotion) {
        return "happy".equalsIgnoreCase(emotion) || "pleased".equalsIgnoreCase(emotion);
    }

    private boolean shouldUseRationalDirectionGate(String beforeEmotion, MecotRationalEmotionVector rationalVector) {
        if (rationalVector == null || !StringUtils.hasText(rationalVector.getRationalEmotion())) {
            return false;
        }
        String rationalEmotion = rationalVector.getRationalEmotion().toLowerCase(Locale.ROOT);
        if (!AiEmotionStateModel.states().containsKey(rationalEmotion) || rationalEmotion.equalsIgnoreCase(beforeEmotion)) {
            return false;
        }
        return rationalVector.getValenceDelta() <= -0.25 && Math.hypot(rationalVector.getValenceDelta(), rationalVector.getArousalDelta()) >= 0.35;
    }

    private String gatedRationalEmotion(Map<String, Double> distribution, String beforeEmotion, MecotRationalEmotionVector rationalVector) {
        String rationalEmotion = rationalVector.getRationalEmotion().toLowerCase(Locale.ROOT);
        AiEmotionStateModel.Point beforePoint = AiEmotionStateModel.point(beforeEmotion);
        AiEmotionStateModel.Point rationalPoint = AiEmotionStateModel.point(rationalEmotion);
        double gateRadius = rationalEmotionGateRadius(beforePoint, rationalPoint);
        return distribution
            .entrySet()
            .stream()
            .filter(entry -> distance(AiEmotionStateModel.point(entry.getKey()), rationalPoint) <= gateRadius)
            .max(
                Comparator.comparingDouble(entry ->
                    entry.getValue() * Math.exp(-3.0 * Math.pow(distance(AiEmotionStateModel.point(entry.getKey()), rationalPoint), 2))
                )
            )
            .map(Map.Entry::getKey)
            .orElse(rationalEmotion);
    }

    private double rationalEmotionGateRadius(AiEmotionStateModel.Point beforePoint, AiEmotionStateModel.Point rationalPoint) {
        return Math.max(0.62, distance(beforePoint, rationalPoint) * 0.55);
    }

    private double distance(AiEmotionStateModel.Point first, AiEmotionStateModel.Point second) {
        return Math.hypot(first.valence() - second.valence(), first.arousal() - second.arousal());
    }

    private List<MecotEmotionCandidate> topCandidates(Map<String, Double> distribution) {
        return distribution
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(TOP_CANDIDATE_LIMIT)
            .map(entry -> new MecotEmotionCandidate(entry.getKey(), scale(entry.getValue())))
            .toList();
    }

    private boolean highNegative(String emotion) {
        return "angry".equalsIgnoreCase(emotion) ||
        "grieved".equalsIgnoreCase(emotion) ||
        "disgusted".equalsIgnoreCase(emotion) ||
        "fearful".equalsIgnoreCase(emotion) ||
        "depressed".equalsIgnoreCase(emotion);
    }

    private boolean containsFinancialStress(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return false;
        }
        String text = userMessage.toLowerCase(Locale.ROOT);
        return text.contains("超支") || text.contains("焦虑") || text.contains("累") || text.contains("压力") || text.contains("麻烦");
    }

    private String buildReason(
        String userEmotionCode,
        String userMessage,
        List<?> memories,
        String beforeEmotion,
        String afterEmotion,
        MecotSelectionStrategy strategy,
        MecotRationalEmotionVector rationalVector
    ) {
        List<String> hints = new ArrayList<>();
        if (containsFinancialStress(userMessage)) {
            hints.add("financial_stress");
        }
        if (containsInvalidation(userMessage)) {
            hints.add("invalidation");
        }
        if (containsPositiveHint(userMessage)) {
            hints.add("positive");
        }
        if (hints.isEmpty()) {
            hints.add("neutral");
        }
        return "userEmotion=%s, before=%s, after=%s, strategy=%s, delta=(%s,%s), rationalEmotion=%s, messageHint=%s, memories=%d".formatted(
            userEmotionCode,
            beforeEmotion,
            afterEmotion,
            strategy,
            scale(rationalVector.getValenceDelta()),
            scale(rationalVector.getArousalDelta()),
            rationalVector.getRationalEmotion(),
            String.join("+", hints),
            memories == null ? 0 : memories.size()
        );
    }

    private boolean containsPositiveHint(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return false;
        }
        if ((userMessage.contains("别") || userMessage.contains("不想") || userMessage.contains("不要")) && userMessage.contains("开心")) {
            return false;
        }
        return userMessage.contains("谢谢") || userMessage.contains("开心") || userMessage.contains("好");
    }

    private AiEmotionStateModel.Point invalidationTarget(String beforeEmotion, String userMessage) {
        if (containsStrongInvalidation(userMessage) || "grieved".equalsIgnoreCase(beforeEmotion) || "sad".equalsIgnoreCase(beforeEmotion)) {
            return AiEmotionStateModel.point("angry");
        }
        return AiEmotionStateModel.point("grieved");
    }

    private boolean containsInvalidation(String userMessage) {
        return containsStrongInvalidation(userMessage) ||
        containsAny(userMessage, "没让你", "你记什么账", "不懂记账", "记了也没用", "摆设", "没用", "乱记", "别记", "不用你记");
    }

    private boolean containsStrongInvalidation(String userMessage) {
        return containsAny(userMessage, "根本就不懂", "记了也没用", "就是摆设", "没用", "废物", "垃圾", "乱来");
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

}
