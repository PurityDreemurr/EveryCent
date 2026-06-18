package com.everycent.assistant.emotion;

public class MecotRationalEmotionVector {

    private double valenceDelta;

    private double arousalDelta;

    private String rationalEmotion;

    private String reason;

    public MecotRationalEmotionVector() {}

    public MecotRationalEmotionVector(double valenceDelta, double arousalDelta, String rationalEmotion, String reason) {
        this.valenceDelta = valenceDelta;
        this.arousalDelta = arousalDelta;
        this.rationalEmotion = rationalEmotion;
        this.reason = reason;
    }

    public double getValenceDelta() {
        return valenceDelta;
    }

    public void setValenceDelta(double valenceDelta) {
        this.valenceDelta = valenceDelta;
    }

    public double getArousalDelta() {
        return arousalDelta;
    }

    public void setArousalDelta(double arousalDelta) {
        this.arousalDelta = arousalDelta;
    }

    public String getRationalEmotion() {
        return rationalEmotion;
    }

    public void setRationalEmotion(String rationalEmotion) {
        this.rationalEmotion = rationalEmotion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "MecotRationalEmotionVector{" +
        "valenceDelta=" +
        valenceDelta +
        ", arousalDelta=" +
        arousalDelta +
        ", rationalEmotion='" +
        rationalEmotion +
        '\'' +
        ", reason='" +
        reason +
        '\'' +
        '}';
    }
}
