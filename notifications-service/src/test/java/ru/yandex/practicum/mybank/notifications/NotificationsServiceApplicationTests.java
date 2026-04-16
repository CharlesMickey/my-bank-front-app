package ru.yandex.practicum.mybank.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notifications;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE SCHEMA IF NOT EXISTS notifications",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=none"
})
class NotificationsServiceApplicationTests {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsAsNonWebKafkaConsumerApplication() {
        assertThat(applicationContext.getEnvironment().getProperty("spring.main.web-application-type")).isEqualTo("none");
        assertThat(applicationContext.containsBean("notificationEventConsumer")).isTrue();
    }
}