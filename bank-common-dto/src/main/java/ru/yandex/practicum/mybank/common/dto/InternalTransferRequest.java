package ru.yandex.practicum.mybank.common.dto;

public record InternalTransferRequest(String recipientLogin, long value) {
}
