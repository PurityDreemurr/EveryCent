package com.everycent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "monthly_balance")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MonthlyBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "year", nullable = false)
    private Integer year;

    @NotNull
    @Min(value = 1)
    @Max(value = 12)
    @Column(name = "month", nullable = false)
    private Integer month;

    @NotNull
    @Column(name = "total_income", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total_expense", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @NotNull
    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "permissions", "transactionRecords", "budgets" }, allowSetters = true)
    private Ledger ledger;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return this.year;
    }

    public MonthlyBalance year(Integer year) {
        this.setYear(year);
        return this;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return this.month;
    }

    public MonthlyBalance month(Integer month) {
        this.setMonth(month);
        return this;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public BigDecimal getTotalIncome() {
        return this.totalIncome;
    }

    public MonthlyBalance totalIncome(BigDecimal totalIncome) {
        this.setTotalIncome(totalIncome);
        return this;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return this.totalExpense;
    }

    public MonthlyBalance totalExpense(BigDecimal totalExpense) {
        this.setTotalExpense(totalExpense);
        return this;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public MonthlyBalance balance(BigDecimal balance) {
        this.setBalance(balance);
        return this;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getUpdatedDate() {
        return this.updatedDate;
    }

    public MonthlyBalance updatedDate(Instant updatedDate) {
        this.setUpdatedDate(updatedDate);
        return this;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    public MonthlyBalance ledger(Ledger ledger) {
        this.setLedger(ledger);
        return this;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MonthlyBalance)) {
            return false;
        }
        return getId() != null && getId().equals(((MonthlyBalance) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "MonthlyBalance{" +
            "id=" + getId() +
            ", year=" + getYear() +
            ", month=" + getMonth() +
            ", totalIncome=" + getTotalIncome() +
            ", totalExpense=" + getTotalExpense() +
            ", balance=" + getBalance() +
            ", updatedDate='" + getUpdatedDate() + "'" +
            "}";
    }
}
