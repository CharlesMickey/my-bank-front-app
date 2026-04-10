package ru.yandex.practicum.mybank.front.client;

import org.springframework.http.HttpStatusCode;

import java.util.List;

public class GatewayClientException extends RuntimeException {
    private final HttpStatusCode status;
    private final List<String> errors;

    public GatewayClientException(HttpStatusCode status, List<String> errors) {
        super(errors == null || errors.isEmpty() ? "Ошибка выполнения операции" : errors.getFirst());
        this.status = status;
        this.errors = errors == null ? List.of(getMessage()) : errors;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }
}
