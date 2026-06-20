package com.everycent.llm.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiAlertResultDTO {

    private String title;

    private String content;

    private String analysisSummary;

    private List<String> majorExpenses = new ArrayList<>();

    private List<String> unnecessaryExpenses = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

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

    public String getAnalysisSummary() {
        return analysisSummary;
    }

    public void setAnalysisSummary(String analysisSummary) {
        this.analysisSummary = analysisSummary;
    }

    public List<String> getMajorExpenses() {
        return majorExpenses;
    }

    public void setMajorExpenses(List<String> majorExpenses) {
        this.majorExpenses = majorExpenses;
    }

    public List<String> getUnnecessaryExpenses() {
        return unnecessaryExpenses;
    }

    public void setUnnecessaryExpenses(List<String> unnecessaryExpenses) {
        this.unnecessaryExpenses = unnecessaryExpenses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
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
