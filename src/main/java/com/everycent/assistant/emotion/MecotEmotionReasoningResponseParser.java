package com.everycent.assistant.emotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MecotEmotionReasoningResponseParser {

    private final ObjectMapper objectMapper;

    public MecotEmotionReasoningResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MecotRationalEmotionVector parse(String response) {
        if (!StringUtils.hasText(response)) {
            return neutral("empty_response");
        }
        try {
            JsonNode root = objectMapper.readTree(extractJson(response));
            return new MecotRationalEmotionVector(
                clamp(root.path("valence_delta").asDouble(0.0)),
                clamp(root.path("arousal_delta").asDouble(0.0)),
                text(root, "rational_emotion", "calm"),
                text(root, "reason", "llm_reasoning")
            );
        } catch (Exception e) {
            return neutral("parse_failed:" + e.getClass().getSimpleName());
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String text(JsonNode root, String field, String defaultValue) {
        String value = root.path(field).asText(defaultValue);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    private MecotRationalEmotionVector neutral(String reason) {
        return new MecotRationalEmotionVector(0.0, 0.0, "calm", reason);
    }
}
