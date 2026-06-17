package com.everycent.llm.dto;

import java.math.BigDecimal;

public class EmotionStatDTO {

    private String emotionTagCode;

    private String emotionTagName;

    private Long count;

    private BigDecimal ratio;

    public String getEmotionTagCode() {
        return emotionTagCode;
    }

    public void setEmotionTagCode(String emotionTagCode) {
        this.emotionTagCode = emotionTagCode;
    }

    public String getEmotionTagName() {
        return emotionTagName;
    }

    public void setEmotionTagName(String emotionTagName) {
        this.emotionTagName = emotionTagName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public BigDecimal getRatio() {
        return ratio;
    }

    public void setRatio(BigDecimal ratio) {
        this.ratio = ratio;
    }
}
