package com.everycent.assistant.dto;

import com.everycent.domain.enumeration.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountingCaptureDTO {

    private Boolean captured;

    private Boolean created;

    private Boolean needConfirmation;

    private Long confirmationId;

    private Long transactionId;

    private BigDecimal amount;

    private TransactionType type;

    private String behaviorTagCode;

    private String emotionTagCode;

    private LocalDate transactionDate;

    private Double confidence;

    public Boolean getCaptured() {
        return captured;
    }

    public void setCaptured(Boolean captured) {
        this.captured = captured;
    }

    public Boolean getCreated() {
        return created;
    }

    public void setCreated(Boolean created) {
        this.created = created;
    }

    public Boolean getNeedConfirmation() {
        return needConfirmation;
    }

    public void setNeedConfirmation(Boolean needConfirmation) {
        this.needConfirmation = needConfirmation;
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

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

    public String getEmotionTagCode() {
        return emotionTagCode;
    }

    public void setEmotionTagCode(String emotionTagCode) {
        this.emotionTagCode = emotionTagCode;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
