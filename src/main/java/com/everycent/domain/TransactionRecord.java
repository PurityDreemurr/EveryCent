package com.everycent.domain;

import com.everycent.domain.enumeration.RecordSource;
import com.everycent.domain.enumeration.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_record")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TransactionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TransactionType type;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30, nullable = false)
    private RecordSource source = RecordSource.MANUAL;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Lob
    @Column(name = "raw_input")
    private String rawInput;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "permissions", "transactionRecords", "budgets" }, allowSetters = true)
    private Ledger ledger;

    @ManyToOne(optional = false)
    @NotNull
    private User creator;

    @ManyToOne
    private BehaviorTag behaviorTag;

    @ManyToOne
    private EmotionTag emotionTag;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public TransactionRecord amount(BigDecimal amount) {
        this.setAmount(amount);
        return this;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return this.type;
    }

    public TransactionRecord type(TransactionType type) {
        this.setType(type);
        return this;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDate getTransactionDate() {
        return this.transactionDate;
    }

    public TransactionRecord transactionDate(LocalDate transactionDate) {
        this.setTransactionDate(transactionDate);
        return this;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public RecordSource getSource() {
        return this.source;
    }

    public TransactionRecord source(RecordSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(RecordSource source) {
        this.source = source;
    }

    public String getDescription() {
        return this.description;
    }

    public TransactionRecord description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRawInput() {
        return this.rawInput;
    }

    public TransactionRecord rawInput(String rawInput) {
        this.setRawInput(rawInput);
        return this;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public TransactionRecord createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public TransactionRecord lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    public TransactionRecord ledger(Ledger ledger) {
        this.setLedger(ledger);
        return this;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    public User getCreator() {
        return this.creator;
    }

    public TransactionRecord creator(User user) {
        this.setCreator(user);
        return this;
    }

    public void setCreator(User user) {
        this.creator = user;
    }

    public BehaviorTag getBehaviorTag() {
        return this.behaviorTag;
    }

    public TransactionRecord behaviorTag(BehaviorTag behaviorTag) {
        this.setBehaviorTag(behaviorTag);
        return this;
    }

    public void setBehaviorTag(BehaviorTag behaviorTag) {
        this.behaviorTag = behaviorTag;
    }

    public EmotionTag getEmotionTag() {
        return this.emotionTag;
    }

    public TransactionRecord emotionTag(EmotionTag emotionTag) {
        this.setEmotionTag(emotionTag);
        return this;
    }

    public void setEmotionTag(EmotionTag emotionTag) {
        this.emotionTag = emotionTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionRecord)) {
            return false;
        }
        return getId() != null && getId().equals(((TransactionRecord) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
            "id=" + getId() +
            ", amount=" + getAmount() +
            ", type='" + getType() + "'" +
            ", transactionDate='" + getTransactionDate() + "'" +
            ", source='" + getSource() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
