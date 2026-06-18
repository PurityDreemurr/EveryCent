package com.everycent.service.dto;

import com.everycent.domain.enumeration.NotificationLevel;
import com.everycent.domain.enumeration.NotificationType;
import java.time.Instant;

public class NotificationMessageDTO {

    private Long id;

    private Long userId;

    private Long ledgerId;

    private Long budgetId;

    private String title;

    private String content;

    private NotificationType type;

    private NotificationLevel level;

    private Boolean read;

    private Instant createdDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
}
