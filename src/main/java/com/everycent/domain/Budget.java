package com.everycent.domain;

import com.everycent.domain.enumeration.BudgetCycle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budget")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Budget implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", length = 20, nullable = false)
    private BudgetCycle cycle;

    @NotNull
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "limit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal limitAmount;

    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "1.00")
    @Column(name = "alert_threshold", precision = 5, scale = 2, nullable = false)
    private BigDecimal alertThreshold = BigDecimal.valueOf(0.80);

    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

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

    public BudgetCycle getCycle() {
        return this.cycle;
    }

    public Budget cycle(BudgetCycle cycle) {
        this.setCycle(cycle);
        return this;
    }

    public void setCycle(BudgetCycle cycle) {
        this.cycle = cycle;
    }

    public LocalDate getPeriodStart() {
        return this.periodStart;
    }

    public Budget periodStart(LocalDate periodStart) {
        this.setPeriodStart(periodStart);
        return this;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return this.periodEnd;
    }

    public Budget periodEnd(LocalDate periodEnd) {
        this.setPeriodEnd(periodEnd);
        return this;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getLimitAmount() {
        return this.limitAmount;
    }

    public Budget limitAmount(BigDecimal limitAmount) {
        this.setLimitAmount(limitAmount);
        return this;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BigDecimal getAlertThreshold() {
        return this.alertThreshold;
    }

    public Budget alertThreshold(BigDecimal alertThreshold) {
        this.setAlertThreshold(alertThreshold);
        return this;
    }

    public void setAlertThreshold(BigDecimal alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public Budget enabled(Boolean enabled) {
        this.setEnabled(enabled);
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    public Budget ledger(Ledger ledger) {
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
        if (!(o instanceof Budget)) {
            return false;
        }
        return getId() != null && getId().equals(((Budget) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Budget{" +
            "id=" + getId() +
            ", cycle='" + getCycle() + "'" +
            ", periodStart='" + getPeriodStart() + "'" +
            ", periodEnd='" + getPeriodEnd() + "'" +
            ", limitAmount=" + getLimitAmount() +
            ", alertThreshold=" + getAlertThreshold() +
            ", enabled='" + getEnabled() + "'" +
            "}";
    }
}
