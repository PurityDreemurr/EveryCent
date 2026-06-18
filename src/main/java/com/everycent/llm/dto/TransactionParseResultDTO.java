package com.everycent.llm.dto;

import com.everycent.domain.enumeration.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionParseResultDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private TransactionType type;

    private String behaviorTagCode;

    private String behaviorTagName;

    private Long behaviorTagId;

    private String emotionTagCode;

    private String emotionTagName;

    private Long emotionTagId;

    private LocalDate transactionDate;

    private String description;

    private Double confidence;

    private Boolean needUserConfirm;

    private String rawInput;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getBehaviorTagCode() {
        return behaviorTagCode;
    }

    public void setBehaviorTagCode(String behaviorTagCode) {
        this.behaviorTagCode = behaviorTagCode;
    }

    public String getBehaviorTagName() {
        return behaviorTagName;
    }

    public void setBehaviorTagName(String behaviorTagName) {
        this.behaviorTagName = behaviorTagName;
    }

    public Long getBehaviorTagId() {
        return behaviorTagId;
    }

    public void setBehaviorTagId(Long behaviorTagId) {
        this.behaviorTagId = behaviorTagId;
    }

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

    public Long getEmotionTagId() {
        return emotionTagId;
    }

    public void setEmotionTagId(Long emotionTagId) {
        this.emotionTagId = emotionTagId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getNeedUserConfirm() {
        return needUserConfirm;
    }

    public void setNeedUserConfirm(Boolean needUserConfirm) {
        this.needUserConfirm = needUserConfirm;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }
}
