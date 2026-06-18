package com.everycent.repository.projection;

import java.math.BigDecimal;

public interface TagAmountProjection {
    Long getTagId();

    String getTagName();

    BigDecimal getAmount();

    Long getCount();
}
