package com.everycent.service.dto;

import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class TransactionRecordDTO {

    private Long id;

    private Long ledgerId;

    @NotNull
    @DecimalMin(value = "0.01")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    private Long behaviorTagId;

    private String behaviorTagName;

    private Long emotionTagId;

    private String emotionTagName;

    @NotNull
    @JsonProperty("recordDate")
    @JsonAlias("transactionDate")
    private LocalDate transactionDate;

    @Size(max = 500)
    private String description;

    private RecordSource source = RecordSource.MANUAL;

    private String rawInput;

    private Long creatorId;

    private Long createdBy;

    private String creatorLogin;

    private Instant createdDate;

    private Instant lastModifiedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
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

    public Long getBehaviorTagId() {
        return behaviorTagId;
    }

    public void setBehaviorTagId(Long behaviorTagId) {
        this.behaviorTagId = behaviorTagId;
    }

    public String getBehaviorTagName() {
        return behaviorTagName;
    }

    public void setBehaviorTagName(String behaviorTagName) {
        this.behaviorTagName = behaviorTagName;
    }

    public Long getEmotionTagId() {
        return emotionTagId;
    }

    public void setEmotionTagId(Long emotionTagId) {
        this.emotionTagId = emotionTagId;
    }

    public String getEmotionTagName() {
        return emotionTagName;
    }

    public void setEmotionTagName(String emotionTagName) {
        this.emotionTagName = emotionTagName;
    }

    @JsonProperty("recordDate")
    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    @JsonProperty("recordDate")
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDate getRecordDate() {
        return transactionDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.transactionDate = recordDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecordSource getSource() {
        return source;
    }

    public void setSource(RecordSource source) {
        this.source = source;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    @JsonProperty("createdBy")
    public Long getCreatedBy() {
        return createdBy;
    }

    @JsonProperty("createdBy")
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorLogin() {
        return creatorLogin;
    }

    public void setCreatorLogin(String creatorLogin) {
        this.creatorLogin = creatorLogin;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
