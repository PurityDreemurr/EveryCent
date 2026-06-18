package com.everycent.assistant.emotion;

import java.math.BigDecimal;

public class MecotEmotionCandidate {

    private String emotion;

    private BigDecimal probability;

    public MecotEmotionCandidate() {}

    public MecotEmotionCandidate(String emotion, BigDecimal probability) {
        this.emotion = emotion;
        this.probability = probability;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(BigDecimal probability) {
        this.probability = probability;
    }

    @Override
    public String toString() {
        return emotion + "=" + probability;
    }
}
