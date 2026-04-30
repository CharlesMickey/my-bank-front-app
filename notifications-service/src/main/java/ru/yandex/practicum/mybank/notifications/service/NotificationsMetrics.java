package ru.yandex.practicum.mybank.notifications.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class NotificationsMetrics {
    private final MeterRegistry meterRegistry;

    public NotificationsMetrics(@Qualifier("prometheusMeterRegistry") MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordFailedNotification(String login) {
        Counter.builder("bank.notifications.failures")
                .description("Number of failed notification deliveries")
                .tag("login", login)
                .register(meterRegistry)
                .increment();
    }
}
