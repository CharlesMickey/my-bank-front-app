package ru.yandex.practicum.mybank.cash.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.yandex.practicum.mybank.cash.error.BankException;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;

import java.time.Duration;

@Component
public class AccountsClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);

    private final WebClient webClient;
    private final String accountsUrl;

    public AccountsClient(
            WebClient serviceWebClient,
            @Value("${BANK_ACCOUNTS_URL:http://localhost:8081}") String accountsUrl
    ) {
        this.webClient = serviceWebClient;
        this.accountsUrl = normalizeUrl(accountsUrl);
    }

    @CircuitBreaker(name = "accounts", fallbackMethod = "fallback")
    public AccountDetailsDto deposit(String login, long value) {
        return call(login, "deposit", value);
    }

    @CircuitBreaker(name = "accounts", fallbackMethod = "fallback")
    public AccountDetailsDto withdraw(String login, long value) {
        return call(login, "withdraw", value);
    }

    private AccountDetailsDto call(String login, String action, long value) {
        return webClient.post()
                .uri(accountsUrl + "/api/internal/accounts/{login}/{action}", login, action)
                .bodyValue(new InternalCashRequest(value))
                .retrieve()
                .bodyToMono(AccountDetailsDto.class)
                .block(REQUEST_TIMEOUT);
    }

    private AccountDetailsDto fallback(String login, long value, Throwable error) {
        if (error instanceof WebClientResponseException responseException) {
            log.warn("Accounts service responded with error during cash operation login={} status={}",
                    login, responseException.getStatusCode().value());
            throw responseException;
        }
        log.error("Accounts service is unavailable during cash operation login={} amount={}", login, value, error);
        throw new BankException(HttpStatus.SERVICE_UNAVAILABLE, "Accounts service is temporarily unavailable");
    }

    private String normalizeUrl(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
