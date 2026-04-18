package ru.yandex.practicum.mybank.common.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.kafka.NotificationPublisher;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class NotificationKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    NotificationPublisher notificationPublisher(
            KafkaTemplate<String, NotificationRequest> kafkaTemplate,
            @Value("${bank.notifications.topic}") String topic
    ) {
        return new NotificationPublisher(kafkaTemplate, topic);
    }
}
