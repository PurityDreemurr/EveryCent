package com.everycent.llm.dto;

import com.everycent.domain.enumeration.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public class NaturalLanguageTransactionCreateResultDTO {

    private Long transactionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private TransactionType type;

    private String behaviorTagName;

    private String emotionTagName;

    private ParsedResultDTO parsedResult;

    private BudgetWarningDTO budgetWarning;

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

    public String getBehaviorTagName() {
        return behaviorTagName;
    }

    public void setBehaviorTagName(String behaviorTagName) {
        this.behaviorTagName = behaviorTagName;
    }

    public String getEmotionTagName() {
        return emotionTagName;
    }

    public void setEmotionTagName(String emotionTagName) {
        this.emotionTagName = emotionTagName;
    }

    public ParsedResultDTO getParsedResult() {
        return parsedResult;
    }

    public void setParsedResult(ParsedResultDTO parsedResult) {
        this.parsedResult = parsedResult;
    }

    public BudgetWarningDTO getBudgetWarning() {
        return budgetWarning;
    }

    public void setBudgetWarning(BudgetWarningDTO budgetWarning) {
        this.budgetWarning = budgetWarning;
    }

    public static class ParsedResultDTO {

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal amount;

        private TransactionType type;

        private String behaviorTag;

        private String emotionTag;

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

        public String getBehaviorTag() {
            return behaviorTag;
        }

        public void setBehaviorTag(String behaviorTag) {
            this.behaviorTag = behaviorTag;
        }

        public String getEmotionTag() {
            return emotionTag;
        }

        public void setEmotionTag(String emotionTag) {
            this.emotionTag = emotionTag;
        }
    }

    public static class BudgetWarningDTO {

        private Boolean overBudget;

        private BigDecimal usedRatio;

        private String message;

        public Boolean getOverBudget() {
            return overBudget;
        }

        public void setOverBudget(Boolean overBudget) {
            this.overBudget = overBudget;
        }

        public BigDecimal getUsedRatio() {
            return usedRatio;
        }

        public void setUsedRatio(BigDecimal usedRatio) {
            this.usedRatio = usedRatio;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
