package ru.yandex.practicum.mybank.cash;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.CashAction;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bank.notifications.topic=bank.notifications"
})
@AutoConfigureObservability
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
@DirtiesContext
class CashNotificationKafkaIntegrationTest {
    private static final String TOPIC = "bank.notifications";

    @Autowired
    private CashService cashService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private AccountsClient accountsClient;

    private Consumer<String, NotificationRequest> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void depositPublishesNotificationToKafka() {
        when(accountsClient.deposit("demo", 50))
                .thenReturn(new AccountDetailsDto("demo", "Ivan Ivanov", LocalDate.of(2001, 1, 1), 150));
        consumer = createConsumer("cash-notifications");

        cashService.process("demo", new CashRequest(50, CashAction.PUT));

        NotificationRequest notification = KafkaTestUtils
                .getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10))
                .value();

        assertThat(notification.login()).isEqualTo("demo");
        assertThat(notification.type()).isEqualTo("CASH_DEPOSIT");
        assertThat(notification.amount()).isEqualTo(50);
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
