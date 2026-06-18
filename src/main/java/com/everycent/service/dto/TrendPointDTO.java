package com.everycent.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TrendPointDTO {

    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal income;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal expense;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }
}
