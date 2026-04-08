package ru.yandex.practicum.mybank.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mybank.notifications.model.NotificationEvent;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
}
