package ru.yandex.practicum.mybank.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.mybank.transfer.error.BankException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.yandex.practicum.mybank.common.dto.ApiError;

import java.io.IOException;
import java.util.List;

@RestControllerAdvice
public class TransferExceptionHandler {
    private final ObjectMapper objectMapper;

    public TransferExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(BankException.class)
    ResponseEntity<ApiError> handleBankException(BankException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return ResponseEntity.badRequest().body(ApiError.of(errors));
    }

    @ExceptionHandler(WebClientResponseException.class)
    ResponseEntity<ApiError> handleRemote(WebClientResponseException exception) {
        ApiError error = readRemoteError(exception);
        return ResponseEntity.status(exception.getStatusCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("Ошибка сервиса переводов: " + exception.getMessage()));
    }

    private ApiError readRemoteError(WebClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ApiError.of(exception.getMessage());
        }
        try {
            return objectMapper.readValue(body, ApiError.class);
        } catch (IOException ignored) {
            return ApiError.of(body);
        }
    }
}
