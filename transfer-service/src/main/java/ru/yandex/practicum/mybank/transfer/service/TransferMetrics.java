package ru.yandex.practicum.mybank.transfer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TransferMetrics {
    private final MeterRegistry meterRegistry;

    public TransferMetrics(@Qualifier("prometheusMeterRegistry") MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordFailedTransfer(String senderLogin, String recipientLogin) {
        Counter.builder("bank.transfer.failures")
                .description("Number of failed transfer attempts")
                .tag("senderLogin", senderLogin)
                .tag("recipientLogin", recipientLogin)
                .register(meterRegistry)
                .increment();
    }
}
