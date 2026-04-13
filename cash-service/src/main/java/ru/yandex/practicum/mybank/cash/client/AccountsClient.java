package ru.yandex.practicum.mybank.cash.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;

import java.time.Duration;

@Component
public class AccountsClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String accountsUrl;

    public AccountsClient(
            WebClient serviceWebClient,
            @Value("${BANK_ACCOUNTS_URL:http://localhost:8081}") String accountsUrl
    ) {
        this.webClient = serviceWebClient;
        this.accountsUrl = normalizeUrl(accountsUrl);
    }

    public AccountDetailsDto deposit(String login, long value) {
        return call(login, "deposit", value);
    }

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

    private String normalizeUrl(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
