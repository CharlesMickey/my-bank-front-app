package ru.yandex.practicum.mybank.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.common.error.BankException;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.NotificationClient;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountsClient accountsClient;
    private final NotificationClient notificationClient;

    public TransferService(AccountsClient accountsClient, NotificationClient notificationClient) {
        this.accountsClient = accountsClient;
        this.notificationClient = notificationClient;
    }

    public OperationResultDto transfer(String fromLogin, TransferRequest request) {
        if (request.value() <= 0) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Сумма должна быть больше нуля");
        }
        if (fromLogin.equals(request.login())) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Нельзя перевести деньги самому себе");
        }

        accountsClient.withdraw(fromLogin, request.value());
        try {
            accountsClient.deposit(request.login(), request.value());
        } catch (RuntimeException exception) {
            compensate(fromLogin, request.value(), exception);
            throw exception;
        }

        String message = "Успешно переведено %d руб. клиенту %s".formatted(request.value(), request.login());
        notificationClient.notify(new NotificationRequest(fromLogin, "TRANSFER_SENT", message, request.value()));
        notificationClient.notify(new NotificationRequest(
                request.login(),
                "TRANSFER_RECEIVED",
                "Получен перевод %d руб. от клиента %s".formatted(request.value(), fromLogin),
                request.value()
        ));
        return new OperationResultDto(message);
    }

    private void compensate(String fromLogin, long value, RuntimeException originalException) {
        try {
            accountsClient.deposit(fromLogin, value);
        } catch (RuntimeException compensationException) {
            log.error("Transfer compensation failed for account {}", fromLogin, compensationException);
            originalException.addSuppressed(compensationException);
        }
    }
}
