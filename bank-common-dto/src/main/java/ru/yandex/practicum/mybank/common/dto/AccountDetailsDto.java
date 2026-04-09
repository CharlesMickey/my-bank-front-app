package ru.yandex.practicum.mybank.common.dto;

import java.time.LocalDate;

public record AccountDetailsDto(
        String login,
        String name,
        LocalDate birthdate,
        long sum
) {
}
