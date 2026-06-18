package com.everycent.domain;

import java.math.BigDecimal;

public class AiEmotionState {

    private String currentEmotion;

    private BigDecimal valence;

    private BigDecimal arousal;

    public String getCurrentEmotion() {
        return currentEmotion;
    }

    public void setCurrentEmotion(String currentEmotion) {
        this.currentEmotion = currentEmotion;
    }

    public BigDecimal getValence() {
        return valence;
    }

    public void setValence(BigDecimal valence) {
        this.valence = valence;
    }

    public BigDecimal getArousal() {
        return arousal;
    }

    public void setArousal(BigDecimal arousal) {
        this.arousal = arousal;
    }
}
