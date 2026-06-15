package com.everycent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ledger")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Ledger implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Size(max = 10)
    @Column(name = "default_currency", length = 10, nullable = false)
    private String defaultCurrency = "CNY";

    @NotNull
    @Column(name = "current_month_balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal currentMonthBalance = BigDecimal.ZERO;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @ManyToOne(optional = false)
    @NotNull
    private User creator;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ledger")
    @JsonIgnoreProperties(value = { "user", "ledger", "invitedBy" }, allowSetters = true)
    private Set<UserLedgerPermission> permissions = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ledger")
    @JsonIgnoreProperties(value = { "ledger", "creator", "behaviorTag", "emotionTag" }, allowSetters = true)
    private Set<TransactionRecord> transactionRecords = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ledger")
    @JsonIgnoreProperties(value = { "ledger" }, allowSetters = true)
    private Set<Budget> budgets = new HashSet<>();

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Ledger name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Ledger description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultCurrency() {
        return this.defaultCurrency;
    }

    public Ledger defaultCurrency(String defaultCurrency) {
        this.setDefaultCurrency(defaultCurrency);
        return this;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public BigDecimal getCurrentMonthBalance() {
        return this.currentMonthBalance;
    }

    public Ledger currentMonthBalance(BigDecimal currentMonthBalance) {
        this.setCurrentMonthBalance(currentMonthBalance);
        return this;
    }

    public void setCurrentMonthBalance(BigDecimal currentMonthBalance) {
        this.currentMonthBalance = currentMonthBalance;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Ledger createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public Ledger lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public User getCreator() {
        return this.creator;
    }

    public Ledger creator(User user) {
        this.setCreator(user);
        return this;
    }

    public void setCreator(User user) {
        this.creator = user;
    }

    public Set<UserLedgerPermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<UserLedgerPermission> permissions) {
        this.permissions = permissions;
    }

    public Set<TransactionRecord> getTransactionRecords() {
        return this.transactionRecords;
    }

    public void setTransactionRecords(Set<TransactionRecord> transactionRecords) {
        this.transactionRecords = transactionRecords;
    }

    public Set<Budget> getBudgets() {
        return this.budgets;
    }

    public void setBudgets(Set<Budget> budgets) {
        this.budgets = budgets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ledger)) {
            return false;
        }
        return getId() != null && getId().equals(((Ledger) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Ledger{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", defaultCurrency='" + getDefaultCurrency() + "'" +
            ", currentMonthBalance=" + getCurrentMonthBalance() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
