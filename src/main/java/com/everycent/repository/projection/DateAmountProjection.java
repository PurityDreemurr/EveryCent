package com.everycent.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DateAmountProjection {
    LocalDate getDate();

    BigDecimal getIncome();

    BigDecimal getExpense();
}
