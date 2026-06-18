package com.everycent.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class TagStatDTO {

    private Long tagId;

    private String tagName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private Long count;

    private BigDecimal ratio;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public BigDecimal getRatio() {
        return ratio;
    }

    public void setRatio(BigDecimal ratio) {
        this.ratio = ratio;
    }

    @JsonProperty("percentage")
    public BigDecimal getPercentage() {
        return ratio;
    }

    @JsonProperty("percentage")
    public void setPercentage(BigDecimal percentage) {
        this.ratio = percentage;
    }
}
