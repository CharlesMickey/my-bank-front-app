package ru.yandex.practicum.mybank.transfer.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;

import java.time.Duration;

@Component
public class AccountsClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public AccountsClient(WebClient serviceWebClient) {
        this.webClient = serviceWebClient;
    }

    public AccountDetailsDto deposit(String login, long value) {
        return call(login, "deposit", value);
    }

    public AccountDetailsDto withdraw(String login, long value) {
        return call(login, "withdraw", value);
    }

    private AccountDetailsDto call(String login, String action, long value) {
        return webClient.post()
                .uri("http://ACCOUNTS-SERVICE/api/internal/accounts/{login}/{action}", login, action)
                .bodyValue(new InternalCashRequest(value))
                .retrieve()
                .bodyToMono(AccountDetailsDto.class)
                .block(REQUEST_TIMEOUT);
    }
}
