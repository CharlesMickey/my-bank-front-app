package ru.yandex.practicum.mybank.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CashRequest(
        @Positive(message = "Сумма должна быть больше нуля")
        long value,
        @NotNull(message = "Тип операции обязателен")
        CashAction action
) {
}
