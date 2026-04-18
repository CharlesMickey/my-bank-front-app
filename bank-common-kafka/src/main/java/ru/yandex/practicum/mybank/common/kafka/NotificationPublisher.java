package ru.yandex.practicum.mybank.common.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NotificationPublisher {
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

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
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish notification event", exception);
        }
    }
}
