package com.everycent.assistant.skill;

public class SkillExecutionContext {

    private Long userId;

    private Long defaultLedgerId;

    private String sessionId;

    private String originalInput;

    private Boolean dryRun = false;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDefaultLedgerId() {
        return defaultLedgerId;
    }

    public void setDefaultLedgerId(Long defaultLedgerId) {
        this.defaultLedgerId = defaultLedgerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }
}
