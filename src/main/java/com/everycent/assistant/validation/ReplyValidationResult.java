package com.everycent.assistant.validation;

import java.util.Collections;
import java.util.List;

public class ReplyValidationResult {

    private final boolean passed;
    private final List<String> violations;

    private ReplyValidationResult(boolean passed, List<String> violations) {
        this.passed = passed;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static ReplyValidationResult pass() {
        return new ReplyValidationResult(true, List.of());
    }

    public static ReplyValidationResult fail(List<String> violations) {
        return new ReplyValidationResult(false, violations);
    }

    public boolean isPassed() {
        return passed;
    }

    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }
}
