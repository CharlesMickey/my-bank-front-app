package ru.yandex.practicum.mybank.transfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.common.kafka.NotificationPublisher;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.error.BankException;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountsClient accountsClient;
    private final NotificationPublisher notificationPublisher;
    private final TransferMetrics transferMetrics;

    public TransferService(
            AccountsClient accountsClient,
            NotificationPublisher notificationPublisher,
            TransferMetrics transferMetrics
    ) {
        this.accountsClient = accountsClient;
        this.notificationPublisher = notificationPublisher;
        this.transferMetrics = transferMetrics;
    }

    public OperationResultDto transfer(String fromLogin, TransferRequest request) {
        if (request.value() <= 0) {
            recordFailedTransfer(fromLogin, request.login(), "Transfer rejected because amount is not positive");
            throw new BankException(HttpStatus.BAD_REQUEST, "\u0421\u0443\u043C\u043C\u0430 \u0434\u043E\u043B\u0436\u043D\u0430 \u0431\u044B\u0442\u044C \u0431\u043E\u043B\u044C\u0448\u0435 \u043D\u0443\u043B\u044F");
        }
        if (fromLogin.equals(request.login())) {
            recordFailedTransfer(fromLogin, request.login(), "Transfer rejected because sender equals recipient");
            throw new BankException(HttpStatus.BAD_REQUEST, "\u041D\u0435\u043B\u044C\u0437\u044F \u043F\u0435\u0440\u0435\u0432\u0435\u0441\u0442\u0438 \u0434\u0435\u043D\u044C\u0433\u0438 \u0441\u0430\u043C\u043E\u043C\u0443 \u0441\u0435\u0431\u0435");
        }

        try {
            accountsClient.transfer(fromLogin, request.login(), request.value());
        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                recordFailedTransfer(fromLogin, request.login(), "Transfer rejected by accounts-service");
            }
            throw exception;
        } catch (BankException exception) {
            if (exception.getStatus().is4xxClientError()) {
                recordFailedTransfer(fromLogin, request.login(), "Transfer rejected in transfer-service");
            }
            throw exception;
        }

        String message = "\u0423\u0441\u043F\u0435\u0448\u043D\u043E \u043F\u0435\u0440\u0435\u0432\u0435\u0434\u0435\u043D\u043E %d \u0440\u0443\u0431. \u043A\u043B\u0438\u0435\u043D\u0442\u0443 %s".formatted(request.value(), request.login());
        notificationPublisher.publish(new NotificationRequest(fromLogin, "TRANSFER_SENT", message, request.value()));
        notificationPublisher.publish(new NotificationRequest(
                request.login(),
                "TRANSFER_RECEIVED",
                "\u041F\u043E\u043B\u0443\u0447\u0435\u043D \u043F\u0435\u0440\u0435\u0432\u043E\u0434 %d \u0440\u0443\u0431. \u043E\u0442 \u043A\u043B\u0438\u0435\u043D\u0442\u0430 %s".formatted(request.value(), fromLogin),
                request.value()
        ));
        log.info("Transfer completed fromLogin={} recipientLogin={} amount={}", fromLogin, request.login(), request.value());
        return new OperationResultDto(message);
    }

    private void recordFailedTransfer(String senderLogin, String recipientLogin, String reason) {
        transferMetrics.recordFailedTransfer(senderLogin, recipientLogin);
        log.warn("Transfer failed senderLogin={} recipientLogin={} reason={}", senderLogin, recipientLogin, reason);
    }
}
