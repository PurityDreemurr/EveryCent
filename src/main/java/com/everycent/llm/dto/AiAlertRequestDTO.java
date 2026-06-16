package com.everycent.llm.dto;

import jakarta.validation.constraints.NotNull;

public class AiAlertRequestDTO {

    @NotNull
    private Long ledgerId;

    @NotNull
    private Long budgetId;

    private Boolean saveAsNotification;

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

    public Boolean getSaveAsNotification() {
        return saveAsNotification;
    }

    public void setSaveAsNotification(Boolean saveAsNotification) {
        this.saveAsNotification = saveAsNotification;
    }
}
