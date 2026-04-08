package ru.yandex.practicum.mybank.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AccountUpdateRequest(
        @NotBlank(message = "Фамилия и имя обязательны")
        String name,
        @NotNull(message = "Дата рождения обязательна")
        LocalDate birthdate
) {
}
