package com.everycent.assistant.dto;

import java.math.BigDecimal;

public class EmotionTransitionDTO {

    private String beforeEmotion;

    private String afterEmotion;

    private BigDecimal beforeValence;

    private BigDecimal beforeArousal;

    private BigDecimal afterValence;

    private BigDecimal afterArousal;

    private String reason;

    public String getBeforeEmotion() {
        return beforeEmotion;
    }

    public void setBeforeEmotion(String beforeEmotion) {
        this.beforeEmotion = beforeEmotion;
    }

    public String getAfterEmotion() {
        return afterEmotion;
    }

    public void setAfterEmotion(String afterEmotion) {
        this.afterEmotion = afterEmotion;
    }

    public BigDecimal getBeforeValence() {
        return beforeValence;
    }

    public void setBeforeValence(BigDecimal beforeValence) {
        this.beforeValence = beforeValence;
    }

    public BigDecimal getBeforeArousal() {
        return beforeArousal;
    }

    public void setBeforeArousal(BigDecimal beforeArousal) {
        this.beforeArousal = beforeArousal;
    }

    public BigDecimal getAfterValence() {
        return afterValence;
    }

    public void setAfterValence(BigDecimal afterValence) {
        this.afterValence = afterValence;
    }

    public BigDecimal getAfterArousal() {
        return afterArousal;
    }

    public void setAfterArousal(BigDecimal afterArousal) {
        this.afterArousal = afterArousal;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
