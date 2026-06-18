package com.everycent.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class DashboardSummaryDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalIncome;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalExpense;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal balance;

    private Long transactionCount;

    private BigDecimal budgetUsedRatio;

    private String budgetAlertLevel;

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    @JsonProperty("incomeTotal")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public BigDecimal getIncomeTotal() {
        return totalIncome;
    }

    @JsonProperty("incomeTotal")
    public void setIncomeTotal(BigDecimal incomeTotal) {
        this.totalIncome = incomeTotal;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    @JsonProperty("expenseTotal")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public BigDecimal getExpenseTotal() {
        return totalExpense;
    }

    @JsonProperty("expenseTotal")
    public void setExpenseTotal(BigDecimal expenseTotal) {
        this.totalExpense = expenseTotal;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getBudgetUsedRatio() {
        return budgetUsedRatio;
    }

    public void setBudgetUsedRatio(BigDecimal budgetUsedRatio) {
        this.budgetUsedRatio = budgetUsedRatio;
    }

    @JsonProperty("budgetUsedRate")
    public BigDecimal getBudgetUsedRate() {
        return budgetUsedRatio;
    }

    @JsonProperty("budgetUsedRate")
    public void setBudgetUsedRate(BigDecimal budgetUsedRate) {
        this.budgetUsedRatio = budgetUsedRate;
    }

    public String getBudgetAlertLevel() {
        return budgetAlertLevel;
    }

    public void setBudgetAlertLevel(String budgetAlertLevel) {
        this.budgetAlertLevel = budgetAlertLevel;
    }
}
