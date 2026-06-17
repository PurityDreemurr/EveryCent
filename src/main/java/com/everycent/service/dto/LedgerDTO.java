package com.everycent.service.dto;

import com.everycent.domain.enumeration.PermissionLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public class LedgerDTO {

    private Long id;

    private String name;

    private String description;

    private String defaultCurrency;

    private BigDecimal currentMonthBalance;

    private Long creatorId;

    private String creatorLogin;

    private PermissionLevel permissionLevel;

    private Instant createdDate;

    private Instant lastModifiedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public BigDecimal getCurrentMonthBalance() {
        return currentMonthBalance;
    }

    public void setCurrentMonthBalance(BigDecimal currentMonthBalance) {
        this.currentMonthBalance = currentMonthBalance;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorLogin() {
        return creatorLogin;
    }

    public void setCreatorLogin(String creatorLogin) {
        this.creatorLogin = creatorLogin;
    }

    @JsonProperty("permissionType")
    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    @JsonProperty("permissionType")
    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
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
