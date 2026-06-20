package com.everycent.assistant.validation;

import java.util.Collections;
import java.util.List;

public class ReplyValidationResult {

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
    }

    private final boolean passed;
    private final List<String> violations;
    private final Severity severity;

    private ReplyValidationResult(boolean passed, List<String> violations, Severity severity) {
        this.passed = passed;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
        this.severity = severity == null ? Severity.LOW : severity;
    }

    public static ReplyValidationResult pass() {
        return new ReplyValidationResult(true, List.of(), Severity.LOW);
    }

    public static ReplyValidationResult fail(List<String> violations) {
        return new ReplyValidationResult(false, violations, inferSeverity(violations));
    }

    public boolean isPassed() {
        return passed;
    }

    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public Severity getSeverity() {
        return severity;
    }

    private static Severity inferSeverity(List<String> violations) {
        if (violations == null || violations.isEmpty()) {
            return Severity.LOW;
        }
        for (String violation : violations) {
            if (
                violation.startsWith("FORBIDDEN_PHRASE:")
                    || violation.startsWith("COLD_EXIT:")
                    || violation.startsWith("ACCOUNTING_LEAK:")
                    || "MISSING_OR_INVALID_JSON_TAIL".equals(violation)
                    || "JSON_PARSE_ERROR".equals(violation)
                    || "MOOD_OUT_OF_RANGE".equals(violation)
                    || "INVALID_EMOJI".equals(violation)
                    || "COLD_TONE_IN_EMOTION_SCENE".equals(violation)
                    || violation.startsWith("INVALIDATE_USER_FEELING")
                    || violation.startsWith("INVALIDATE_LONELINESS")
                    || violation.startsWith("USER_BELITTLING")
                    || violation.startsWith("SELF_CENTERED_REPLY")
                    || violation.startsWith("THIRD_PARTY_MOCKING")
                    || violation.startsWith("COMMANDING_TONE")
                    || violation.startsWith("ROLEPLAY_LEAK")
            ) {
                return Severity.HIGH;
            }
        }
        return Severity.MEDIUM;
    }
}
