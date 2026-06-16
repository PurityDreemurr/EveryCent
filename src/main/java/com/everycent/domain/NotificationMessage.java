package com.everycent.domain;

import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "notification_message")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30, nullable = false)
    private NotificationType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 30, nullable = false)
    private NotificationLevel level;

    @NotNull
    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @ManyToOne
    @JsonIgnoreProperties(value = { "permissions", "transactionRecords", "budgets" }, allowSetters = true)
    private Ledger ledger;

    @ManyToOne
    @JsonIgnoreProperties(value = { "ledger" }, allowSetters = true)
    private Budget budget;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public NotificationMessage title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public NotificationMessage content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public NotificationType getType() {
        return this.type;
    }

    public NotificationMessage type(NotificationType type) {
        this.setType(type);
        return this;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationLevel getLevel() {
        return this.level;
    }

    public NotificationMessage level(NotificationLevel level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    public Boolean getRead() {
        return this.read;
    }

    public NotificationMessage read(Boolean read) {
        this.setRead(read);
        return this;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public NotificationMessage createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public User getUser() {
        return this.user;
    }

    public NotificationMessage user(User user) {
        this.setUser(user);
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    public NotificationMessage ledger(Ledger ledger) {
        this.setLedger(ledger);
        return this;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    public Budget getBudget() {
        return this.budget;
    }

    public NotificationMessage budget(Budget budget) {
        this.setBudget(budget);
        return this;
    }

    public void setBudget(Budget budget) {
        this.budget = budget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationMessage)) {
            return false;
        }
        return getId() != null && getId().equals(((NotificationMessage) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "NotificationMessage{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", type='" + getType() + "'" +
            ", level='" + getLevel() + "'" +
            ", read='" + getRead() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
