package com.everycent.domain;

import com.everycent.domain.enumeration.PermissionLevel;
import com.everycent.domain.enumeration.PermissionStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "user_ledger_permission")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserLedgerPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", length = 30, nullable = false)
    private PermissionLevel permissionLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PermissionStatus status = PermissionStatus.ACTIVE;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "permissions", "transactionRecords", "budgets" }, allowSetters = true)
    private Ledger ledger;

    @ManyToOne
    private User invitedBy;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PermissionLevel getPermissionLevel() {
        return this.permissionLevel;
    }

    public UserLedgerPermission permissionLevel(PermissionLevel permissionLevel) {
        this.setPermissionLevel(permissionLevel);
        return this;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public PermissionStatus getStatus() {
        return this.status;
    }

    public UserLedgerPermission status(PermissionStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(PermissionStatus status) {
        this.status = status;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public UserLedgerPermission createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public User getUser() {
        return this.user;
    }

    public UserLedgerPermission user(User user) {
        this.setUser(user);
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    public UserLedgerPermission ledger(Ledger ledger) {
        this.setLedger(ledger);
        return this;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    public User getInvitedBy() {
        return this.invitedBy;
    }

    public UserLedgerPermission invitedBy(User user) {
        this.setInvitedBy(user);
        return this;
    }

    public void setInvitedBy(User user) {
        this.invitedBy = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserLedgerPermission)) {
            return false;
        }
        return getId() != null && getId().equals(((UserLedgerPermission) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserLedgerPermission{" +
            "id=" + getId() +
            ", permissionLevel='" + getPermissionLevel() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
