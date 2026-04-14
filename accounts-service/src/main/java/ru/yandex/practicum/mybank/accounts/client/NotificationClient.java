package ru.yandex.practicum.mybank.accounts.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;

import java.time.Duration;

@Component
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String notificationsUrl;

    public NotificationClient(
            WebClient serviceWebClient,
            @Value("${BANK_NOTIFICATIONS_URL:http://localhost:8084}") String notificationsUrl
    ) {
        this.webClient = serviceWebClient;
        this.notificationsUrl = normalizeUrl(notificationsUrl);
    }

    @CircuitBreaker(name = "notifications", fallbackMethod = "fallback")
    public void notify(NotificationRequest request) {
        webClient.post()
                .uri(notificationsUrl + "/api/notifications")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block(REQUEST_TIMEOUT);
    }

    private void fallback(NotificationRequest request, Throwable error) {
        log.warn("Notification service is unavailable for event {}: {}", request.type(), error.getMessage());
    }

    private String normalizeUrl(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
