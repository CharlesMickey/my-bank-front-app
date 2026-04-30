package ru.yandex.practicum.mybank.common.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NotificationPublisher {
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);
    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private final String topic;

    public NotificationPublisher(KafkaTemplate<String, NotificationRequest> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(NotificationRequest request) {
        try {
            kafkaTemplate.send(topic, request.login(), request)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            log.info("Notification published login={} type={} topic={}", request.login(), request.type(), topic);
        } catch (Exception exception) {
            log.error("Failed to publish notification login={} type={} topic={}", request.login(), request.type(), topic, exception);
            throw new IllegalStateException("Failed to publish notification event", exception);
        }
    }
}
