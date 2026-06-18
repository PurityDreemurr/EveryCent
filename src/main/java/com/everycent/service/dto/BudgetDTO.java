package com.everycent.service.dto;

import com.everycent.domain.enumeration.BudgetCycle;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetDTO {

    private Long id;

    private Long ledgerId;

    @NotNull
    private BudgetCycle cycle;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    @NotNull
    @DecimalMin(value = "0.01")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonAlias("amount")
    private BigDecimal limitAmount;

    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "1.00")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal alertThreshold = BigDecimal.valueOf(0.80);

    private Boolean enabled = true;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal usedAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal remainingAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal usedRatio;

    private String status;

    private Boolean overBudget;

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

    public BudgetCycle getCycle() {
        return cycle;
    }

    public void setCycle(BudgetCycle cycle) {
        this.cycle = cycle;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    @JsonProperty("amount")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public BigDecimal getAmount() {
        return limitAmount;
    }

    @JsonProperty("amount")
    public void setAmount(BigDecimal amount) {
        this.limitAmount = amount;
    }

    @JsonProperty("budgetAmount")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public BigDecimal getBudgetAmount() {
        return limitAmount;
    }

    @JsonProperty("budgetAmount")
    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.limitAmount = budgetAmount;
    }

    public BigDecimal getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(BigDecimal alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    @JsonProperty("usageRate")
    public BigDecimal getUsageRate() {
        return usedRatio;
    }

    @JsonProperty("usageRate")
    public void setUsageRate(BigDecimal usageRate) {
        this.usedRatio = usageRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getOverBudget() {
        return overBudget;
    }

    public void setOverBudget(Boolean overBudget) {
        this.overBudget = overBudget;
    }
}
