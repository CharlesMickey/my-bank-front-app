package ru.yandex.practicum.mybank.cash.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.NotificationClient;
import ru.yandex.practicum.mybank.cash.error.BankException;
import ru.yandex.practicum.mybank.common.dto.CashAction;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;

@Service
public class CashService {
    private final AccountsClient accountsClient;
    private final NotificationClient notificationClient;

    public CashService(AccountsClient accountsClient, NotificationClient notificationClient) {
        this.accountsClient = accountsClient;
        this.notificationClient = notificationClient;
    }

    public OperationResultDto process(String login, CashRequest request) {
        if (request.value() <= 0) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Сумма должна быть больше нуля");
        }

        if (request.action() == CashAction.GET) {
            accountsClient.withdraw(login, request.value());
            String message = "Снято %d руб.".formatted(request.value());
            notificationClient.notify(new NotificationRequest(login, "CASH_WITHDRAW", message, request.value()));
            return new OperationResultDto(message);
        }

        accountsClient.deposit(login, request.value());
        String message = "Положено %d руб.".formatted(request.value());
        notificationClient.notify(new NotificationRequest(login, "CASH_DEPOSIT", message, request.value()));
        return new OperationResultDto(message);
    }
}
