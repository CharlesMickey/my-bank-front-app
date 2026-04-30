package ru.yandex.practicum.mybank.notifications.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.service.NotificationService;

@Component
public class NotificationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${bank.notifications.topic}")
    public void consume(NotificationRequest request) {
        log.info("Kafka notification received login={} type={}", request.login(), request.type());
        notificationService.notify(request);
    }
}
