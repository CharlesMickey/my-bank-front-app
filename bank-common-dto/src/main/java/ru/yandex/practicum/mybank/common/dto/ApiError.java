package ru.yandex.practicum.mybank.common.dto;

import java.util.List;

public record ApiError(String message, List<String> errors) {

    public ApiError(String message) {
        this(message, List.of(message));
    }

    public static ApiError of(String message) {
        return new ApiError(message);
    }

    public static ApiError of(List<String> errors) {
        String message = errors == null || errors.isEmpty() ? "Ошибка выполнения операции" : errors.getFirst();
        return new ApiError(message, errors == null ? List.of(message) : errors);
    }
}
