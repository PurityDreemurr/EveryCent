package com.everycent.service.dto;

import com.everycent.domain.enumeration.TransactionType;
import java.time.LocalDate;

public class TransactionQueryDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    private TransactionType type;

    private Long behaviorTagId;

    private Long emotionTagId;

    private int page = 0;

    private int size = 20;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Long getBehaviorTagId() {
        return behaviorTagId;
    }

    public void setBehaviorTagId(Long behaviorTagId) {
        this.behaviorTagId = behaviorTagId;
    }

    public Long getEmotionTagId() {
        return emotionTagId;
    }

    public void setEmotionTagId(Long emotionTagId) {
        this.emotionTagId = emotionTagId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if (size <= 0) {
            this.size = 20;
        } else {
            this.size = Math.min(size, 100);
        }
    }
}
