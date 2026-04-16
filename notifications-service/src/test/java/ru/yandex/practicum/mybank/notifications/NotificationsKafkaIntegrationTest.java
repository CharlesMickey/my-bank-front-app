package ru.yandex.practicum.mybank.notifications;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.model.NotificationEvent;
import ru.yandex.practicum.mybank.notifications.repository.NotificationEventRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notifications-kafka-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE SCHEMA IF NOT EXISTS notifications",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=notifications-kafka-test",
        "bank.notifications.topic=bank.notifications"
})
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
@DirtiesContext
class NotificationsKafkaIntegrationTest {
    private static final String TOPIC = "bank.notifications";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationEventRepository repository;

    @Test
    void consumesNotificationFromKafkaAndPersistsEvent() throws Exception {
        KafkaTemplate<String, NotificationRequest> kafkaTemplate = createKafkaTemplate();
        try {
            kafkaTemplate.send(TOPIC, "demo", new NotificationRequest("demo", "TEST", "Kafka notification", 10L)).get();
        } finally {
            kafkaTemplate.destroy();
        }

        NotificationEvent savedEvent = waitForEvent("demo", "TEST");

        assertThat(savedEvent.getMessage()).isEqualTo("Kafka notification");
        assertThat(savedEvent.getAmount()).isEqualTo(10L);
    }

    private KafkaTemplate<String, NotificationRequest> createKafkaTemplate() {
        Map<String, Object> properties = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties));
    }

    private NotificationEvent waitForEvent(String login, String type) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            NotificationEvent event = repository.findAll().stream()
                    .filter(candidate -> candidate.getLogin().equals(login))
                    .filter(candidate -> candidate.getType().equals(type))
                    .findFirst()
                    .orElse(null);
            if (event != null) {
                return event;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Notification event was not consumed from Kafka in time");
    }
}
