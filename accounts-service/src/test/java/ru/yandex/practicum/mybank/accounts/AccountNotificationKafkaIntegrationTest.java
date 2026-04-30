package ru.yandex.practicum.mybank.accounts;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.mybank.accounts.service.AccountService;
import ru.yandex.practicum.mybank.common.dto.AccountUpdateRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:accounts-kafka-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE SCHEMA IF NOT EXISTS accounts",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bank.notifications.topic=bank.notifications"
})
@AutoConfigureObservability
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
@DirtiesContext
class AccountNotificationKafkaIntegrationTest {
    private static final String TOPIC = "bank.notifications";

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, NotificationRequest> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void updateCurrentAccountPublishesNotificationToKafka() {
        consumer = createConsumer("accounts-notifications");

        accountService.updateCurrentAccount("demo", new AccountUpdateRequest("Demo User", LocalDate.of(1990, 1, 1)));

        NotificationRequest notification = KafkaTestUtils
                .getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10))
                .value();

        assertThat(notification.login()).isEqualTo("demo");
        assertThat(notification.type()).isEqualTo("ACCOUNT_UPDATED");
        assertThat(notification.message()).contains("\u043E\u0431\u043D\u043E\u0432\u043B\u0435\u043D");
    }

    private Consumer<String, NotificationRequest> createConsumer(String groupId) {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBroker);
        JsonDeserializer<NotificationRequest> valueDeserializer = new JsonDeserializer<>(NotificationRequest.class);
        valueDeserializer.addTrustedPackages("ru.yandex.practicum.mybank.common.dto");
        Consumer<String, NotificationRequest> kafkaConsumer = new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        ).createConsumer();
        embeddedKafkaBroker.consumeFromEmbeddedTopics(kafkaConsumer, TOPIC);
        return kafkaConsumer;
    }
}
