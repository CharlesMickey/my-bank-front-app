package ru.yandex.practicum.mybank.common.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
        @NotBlank(message = "Логин обязателен")
        String login,
        @NotBlank(message = "Тип уведомления обязателен")
        String type,
        @NotBlank(message = "Текст уведомления обязателен")
        String message,
        Long amount
) {
}
