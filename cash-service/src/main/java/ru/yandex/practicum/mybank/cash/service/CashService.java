package ru.yandex.practicum.mybank.cash.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.error.BankException;
import ru.yandex.practicum.mybank.common.dto.CashAction;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;
import ru.yandex.practicum.mybank.common.kafka.NotificationPublisher;

@Service
public class CashService {
    private final AccountsClient accountsClient;
    private final NotificationPublisher notificationPublisher;

    public CashService(AccountsClient accountsClient, NotificationPublisher notificationPublisher) {
        this.accountsClient = accountsClient;
        this.notificationPublisher = notificationPublisher;
    }

    public OperationResultDto process(String login, CashRequest request) {
        if (request.value() <= 0) {
            throw new BankException(HttpStatus.BAD_REQUEST,
                    "\u0421\u0443\u043C\u043C\u0430 \u0434\u043E\u043B\u0436\u043D\u0430 \u0431\u044B\u0442\u044C \u0431\u043E\u043B\u044C\u0448\u0435 \u043D\u0443\u043B\u044F");
        }

        if (request.action() == CashAction.GET) {
            accountsClient.withdraw(login, request.value());
            String message = "\u0421\u043D\u044F\u0442\u043E %d \u0440\u0443\u0431.".formatted(request.value());
            notificationPublisher.publish(new NotificationRequest(login, "CASH_WITHDRAW", message, request.value()));
            return new OperationResultDto(message);
        }

        accountsClient.deposit(login, request.value());
        String message = "\u041F\u043E\u043B\u043E\u0436\u0435\u043D\u043E %d \u0440\u0443\u0431.".formatted(request.value());
        notificationPublisher.publish(new NotificationRequest(login, "CASH_DEPOSIT", message, request.value()));
        return new OperationResultDto(message);
    }
}