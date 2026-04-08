package ru.yandex.practicum.mybank.front.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.AccountDto;
import ru.yandex.practicum.mybank.common.dto.AccountUpdateRequest;
import ru.yandex.practicum.mybank.common.dto.ApiError;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class FrontGatewayClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FrontGatewayClient(RestClient gatewayRestClient, ObjectMapper objectMapper) {
        this.restClient = gatewayRestClient;
        this.objectMapper = objectMapper;
    }

    public AccountPage loadPage(String token) {
        AccountDetailsDto account = getAccount(token);
        List<AccountDto> accounts = getTransferRecipients(token);
        return new AccountPage(account, accounts);
    }

    public AccountDetailsDto getAccount(String token) {
        return restClient.get()
                .uri("/api/accounts/me")
                .headers(bearer(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(AccountDetailsDto.class);
    }

    public List<AccountDto> getTransferRecipients(String token) {
        AccountDto[] accounts = restClient.get()
                .uri("/api/accounts/recipients")
                .headers(bearer(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(AccountDto[].class);
        return accounts == null ? List.of() : Arrays.asList(accounts);
    }

    public AccountDetailsDto updateAccount(String token, AccountUpdateRequest request) {
        return restClient.put()
                .uri("/api/accounts/me")
                .headers(bearer(token))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(AccountDetailsDto.class);
    }

    public OperationResultDto cash(String token, CashRequest request) {
        return restClient.post()
                .uri("/api/cash")
                .headers(bearer(token))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(OperationResultDto.class);
    }

    public OperationResultDto transfer(String token, TransferRequest request) {
        return restClient.post()
                .uri("/api/transfers")
                .headers(bearer(token))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(OperationResultDto.class);
    }

    private Consumer<HttpHeaders> bearer(String token) {
        return headers -> headers.setBearerAuth(token);
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        ApiError error = tryReadError(body);
        List<String> errors = error == null ? List.of(defaultMessage(response.getStatusCode())) : error.errors();
        throw new GatewayClientException(response.getStatusCode(), errors);
    }

    private ApiError tryReadError(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, ApiError.class);
        } catch (IOException ignored) {
            return new ApiError(body);
        }
    }

    private String defaultMessage(HttpStatusCode status) {
        return "Сервис временно недоступен, код ответа: " + status.value();
    }
}
