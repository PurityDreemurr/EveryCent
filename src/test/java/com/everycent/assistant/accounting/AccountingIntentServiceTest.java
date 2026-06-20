package com.everycent.assistant.accounting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountingIntentServiceTest {

    private final AccountingIntentService service = new AccountingIntentService();

    @Test
    void shouldAcceptExplicitAccountingRequests() {
        assertThat(service.isAccountingIntent("帮我记一下外卖 28")).isTrue();
        assertThat(service.isAccountingIntent("午饭28，记到账本1")).isTrue();
        assertThat(service.isAccountingIntent("记录一下今天这笔支出")).isTrue();
    }

    @Test
    void shouldAcceptAmountWithFinanceAction() {
        assertThat(service.isAccountingIntent("外卖花了28，结果还送错了")).isTrue();
        assertThat(service.isAccountingIntent("工资到账5000")).isTrue();
        assertThat(service.isAccountingIntent("报销 120 元")).isTrue();
        assertThat(service.isAccountingIntent("买了咖啡18块")).isTrue();
    }

    @Test
    void shouldAcceptShortTransactionTextWithAmounts() {
        assertThat(service.isAccountingIntent("午饭28，咖啡18")).isTrue();
        assertThat(service.isAccountingIntent("地铁6")).isTrue();
        assertThat(service.isAccountingIntent("coffee 18")).isTrue();
    }

    @Test
    void shouldRejectLifeComplaintsWithoutAccountingSignal() {
        assertThat(service.isAccountingIntent("外卖又送错了，真的无语")).isFalse();
        assertThat(service.isAccountingIntent("今天午饭挺难吃")).isFalse();
        assertThat(service.isAccountingIntent("咖啡太苦了")).isFalse();
        assertThat(service.isAccountingIntent("电影看到一半睡着了")).isFalse();
    }
}
