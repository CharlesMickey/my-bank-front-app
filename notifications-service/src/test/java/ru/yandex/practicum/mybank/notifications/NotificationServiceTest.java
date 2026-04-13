package ru.yandex.practicum.mybank.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notifications-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE SCHEMA IF NOT EXISTS notifications",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationServiceTest {
    @Autowired
    private NotificationService notificationService;

    @Test
    void savesNotificationEvent() {
        var event = notificationService.notify(new NotificationRequest("demo", "TEST", "Проверка", 10L));

        assertThat(event.getId()).isNotNull();
        assertThat(event.getLogin()).isEqualTo("demo");
    }
}
