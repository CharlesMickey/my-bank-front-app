package ru.yandex.practicum.mybank.transfer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.service.TransferService;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bank.notifications.topic=bank.notifications"
})
@EmbeddedKafka(partitions = 1, topics = "bank.notifications")
@DirtiesContext
class TransferNotificationKafkaIntegrationTest {
    private static final String TOPIC = "bank.notifications";

    @Autowired
    private TransferService transferService;

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
    void transferPublishesNotificationToKafka() {
        when(accountsClient.transfer("demo", "petrov", 25))
                .thenReturn(new AccountDetailsDto("demo", "Ivan Ivanov", LocalDate.of(2001, 1, 1), 75));
        consumer = createConsumer("transfer-notifications");

        transferService.transfer("demo", new TransferRequest("petrov", 25));

        ConsumerRecords<String, NotificationRequest> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        List<NotificationRequest> notifications = new ArrayList<>();
        for (ConsumerRecord<String, NotificationRequest> record : records.records(TOPIC)) {
            notifications.add(record.value());
        }

        assertThat(notifications)
                .hasSize(2)
                .extracting(NotificationRequest::type)
                .containsExactlyInAnyOrder("TRANSFER_SENT", "TRANSFER_RECEIVED");
        assertThat(notifications)
                .extracting(NotificationRequest::amount)
                .containsOnly(25L);
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
