package ru.yandex.practicum.mybank.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.model.NotificationEvent;
import ru.yandex.practicum.mybank.notifications.repository.NotificationEventRepository;

import java.time.Instant;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationEventRepository repository;
    private final NotificationsMetrics notificationsMetrics;
    private final ObservationRegistry observationRegistry;

    public NotificationService(
            NotificationEventRepository repository,
            NotificationsMetrics notificationsMetrics,
            ObservationRegistry observationRegistry
    ) {
        this.repository = repository;
        this.notificationsMetrics = notificationsMetrics;
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    public NotificationEvent notify(NotificationRequest request) {
        try {
            NotificationEvent event = observeDatabase("notifications-service db save notification", () -> repository.save(new NotificationEvent(
                    request.login(),
                    request.type(),
                    request.message(),
                    request.amount(),
                    Instant.now()
            )));
            log.info("Notification stored login={} type={} amount={}", request.login(), request.type(), request.amount());
            return event;
        } catch (RuntimeException exception) {
            notificationsMetrics.recordFailedNotification(request.login());
            log.error("Notification delivery failed login={} type={}", request.login(), request.type(), exception);
            throw exception;
        }
    }

    private <T> T observeDatabase(String contextualName, java.util.function.Supplier<T> operation) {
        return Observation.createNotStarted("bank.db.operation", observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("db.system", "postgresql")
                .lowCardinalityKeyValue("service", "notifications-service")
                .observe(operation);
    }
}
