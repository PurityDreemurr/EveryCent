package com.everycent.llm.dto;

import java.math.BigDecimal;

public class AiAlertResultDTO {

    private String title;

    private String content;

    private AlertLevel level;

    private Boolean overBudget;

    private BigDecimal usedAmount;

    private BigDecimal limitAmount;

    private BigDecimal usedRatio;

    private Boolean needNotification;

    private Long notificationId;

    public enum AlertLevel {
        INFO,
        WARNING,
        DANGER,
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

    public AlertLevel getLevel() {
        return level;
    }

    public void setLevel(AlertLevel level) {
        this.level = level;
    }

    public Boolean getOverBudget() {
        return overBudget;
    }

    public void setOverBudget(Boolean overBudget) {
        this.overBudget = overBudget;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BigDecimal getUsedRatio() {
        return usedRatio;
    }

    public void setUsedRatio(BigDecimal usedRatio) {
        this.usedRatio = usedRatio;
    }

    public Boolean getNeedNotification() {
        return needNotification;
    }

    public void setNeedNotification(Boolean needNotification) {
        this.needNotification = needNotification;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
}
