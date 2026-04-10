package ru.yandex.practicum.mybank.notifications.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;
import ru.yandex.practicum.mybank.notifications.service.NotificationService;

@RestController
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/api/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OperationResultDto notify(@Valid @RequestBody NotificationRequest request) {
        notificationService.notify(request);
        return new OperationResultDto("Уведомление принято");
    }
}
