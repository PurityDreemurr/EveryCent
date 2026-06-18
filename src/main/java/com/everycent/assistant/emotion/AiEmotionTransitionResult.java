package com.everycent.assistant.emotion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiEmotionTransitionResult {

    private String beforeEmotion;

    private String afterEmotion;

    private BigDecimal beforeValence;

    private BigDecimal beforeArousal;

    private BigDecimal afterValence;

    private BigDecimal afterArousal;

    private String userEmotionTagCode;

    private MecotSelectionStrategy selectionStrategy;

    private BigDecimal rationalDeltaValence;

    private BigDecimal rationalDeltaArousal;

    private String personalityProfile;

    private List<MecotEmotionCandidate> topCandidates = new ArrayList<>();

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

    public String getUserEmotionTagCode() {
        return userEmotionTagCode;
    }

    public void setUserEmotionTagCode(String userEmotionTagCode) {
        this.userEmotionTagCode = userEmotionTagCode;
    }

    public MecotSelectionStrategy getSelectionStrategy() {
        return selectionStrategy;
    }

    public void setSelectionStrategy(MecotSelectionStrategy selectionStrategy) {
        this.selectionStrategy = selectionStrategy;
    }

    public BigDecimal getRationalDeltaValence() {
        return rationalDeltaValence;
    }

    public void setRationalDeltaValence(BigDecimal rationalDeltaValence) {
        this.rationalDeltaValence = rationalDeltaValence;
    }

    public BigDecimal getRationalDeltaArousal() {
        return rationalDeltaArousal;
    }

    public void setRationalDeltaArousal(BigDecimal rationalDeltaArousal) {
        this.rationalDeltaArousal = rationalDeltaArousal;
    }

    public String getPersonalityProfile() {
        return personalityProfile;
    }

    public void setPersonalityProfile(String personalityProfile) {
        this.personalityProfile = personalityProfile;
    }

    public List<MecotEmotionCandidate> getTopCandidates() {
        return topCandidates;
    }

    public void setTopCandidates(List<MecotEmotionCandidate> topCandidates) {
        this.topCandidates = topCandidates == null ? new ArrayList<>() : topCandidates;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
