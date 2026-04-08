package ru.yandex.practicum.mybank.common.dto;

import jakarta.validation.constraints.Positive;

public record InternalCashRequest(
        @Positive(message = "Сумма должна быть больше нуля")
        long value
) {
}
