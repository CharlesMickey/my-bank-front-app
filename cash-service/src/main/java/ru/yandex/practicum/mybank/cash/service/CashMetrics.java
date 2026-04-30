package ru.yandex.practicum.mybank.cash.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CashMetrics {
    private final MeterRegistry meterRegistry;

    public CashMetrics(@Qualifier("prometheusMeterRegistry") MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordFailedWithdrawal(String login) {
        Counter.builder("bank.cash.withdraw.failures")
                .description("Number of failed withdrawal attempts")
                .tag("login", login)
                .register(meterRegistry)
                .increment();
    }
}
