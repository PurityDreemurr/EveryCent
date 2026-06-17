package com.everycent.llm.dto;

import java.math.BigDecimal;

public class BudgetStatusDTO {

    private BigDecimal limitAmount;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    private BigDecimal usedRatio;

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getUsedRatio() {
        return usedRatio;
    }

    public void setUsedRatio(BigDecimal usedRatio) {
        this.usedRatio = usedRatio;
    }
}
