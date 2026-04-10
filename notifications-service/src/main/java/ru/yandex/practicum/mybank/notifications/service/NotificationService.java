package ru.yandex.practicum.mybank.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.notifications.model.NotificationEvent;
import ru.yandex.practicum.mybank.notifications.repository.NotificationEventRepository;

import java.time.Instant;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationEventRepository repository;

    public NotificationService(NotificationEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationEvent notify(NotificationRequest request) {
        NotificationEvent event = repository.save(new NotificationEvent(
                request.login(),
                request.type(),
                request.message(),
                request.amount(),
                Instant.now()
        ));
        log.info("Bank notification for {} [{}]: {}", request.login(), request.type(), request.message());
        return event;
    }
}
