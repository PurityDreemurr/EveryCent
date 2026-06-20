package com.everycent.assistant.skill;

public class ActionPolicyDecision {

    private final boolean allowed;

    private final boolean needConfirmation;

    private final RiskLevel riskLevel;

    private final String errorCode;

    private final String message;

    private ActionPolicyDecision(boolean allowed, boolean needConfirmation, RiskLevel riskLevel, String errorCode, String message) {
        this.allowed = allowed;
        this.needConfirmation = needConfirmation;
        this.riskLevel = riskLevel;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static ActionPolicyDecision allowed(RiskLevel riskLevel) {
        return new ActionPolicyDecision(true, false, riskLevel, null, null);
    }

    public static ActionPolicyDecision needConfirmation(RiskLevel riskLevel, String message) {
        return new ActionPolicyDecision(false, true, riskLevel, "NEED_CONFIRMATION", message);
    }

    public static ActionPolicyDecision blocked(RiskLevel riskLevel, String errorCode, String message) {
        return new ActionPolicyDecision(false, false, riskLevel, errorCode, message);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isNeedConfirmation() {
        return needConfirmation;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
