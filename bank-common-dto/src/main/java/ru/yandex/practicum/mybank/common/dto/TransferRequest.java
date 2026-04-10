package ru.yandex.practicum.mybank.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank(message = "Получатель обязателен")
        String login,
        @Positive(message = "Сумма должна быть больше нуля")
        long value
) {
}
