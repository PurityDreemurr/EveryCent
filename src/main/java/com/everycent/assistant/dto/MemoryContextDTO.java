package com.everycent.assistant.dto;

public class MemoryContextDTO {

    private Long memoryId;

    private String content;

    private String memoryType;

    private String userEmotionTagCode;

    private Double score;

    public Long getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Long memoryId) {
        this.memoryId = memoryId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public String getUserEmotionTagCode() {
        return userEmotionTagCode;
    }

    public void setUserEmotionTagCode(String userEmotionTagCode) {
        this.userEmotionTagCode = userEmotionTagCode;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
