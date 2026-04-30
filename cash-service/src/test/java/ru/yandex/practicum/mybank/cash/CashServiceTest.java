package ru.yandex.practicum.mybank.cash;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.service.CashMetrics;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.CashAction;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.kafka.NotificationPublisher;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CashServiceTest {

    @Test
    void depositCallsAccountsServiceAndPublishesNotification() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationPublisher notificationPublisher = mock(NotificationPublisher.class);
        CashMetrics cashMetrics = mock(CashMetrics.class);
        when(accountsClient.deposit("demo", 50))
                .thenReturn(new AccountDetailsDto("demo", "Ivan Ivanov", LocalDate.of(2001, 1, 1), 150));
        CashService cashService = new CashService(accountsClient, notificationPublisher, cashMetrics);

        var result = cashService.process("demo", new CashRequest(50, CashAction.PUT));
        ArgumentCaptor<NotificationRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationRequest.class);

        assertThat(result.message()).isEqualTo("\u041F\u043E\u043B\u043E\u0436\u0435\u043D\u043E 50 \u0440\u0443\u0431.");
        verify(accountsClient).deposit("demo", 50);
        verify(notificationPublisher).publish(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().type()).isEqualTo("CASH_DEPOSIT");
        assertThat(notificationCaptor.getValue().amount()).isEqualTo(50);
    }

    @Test
    void withdrawalCallsAccountsServiceAndPublishesNotification() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationPublisher notificationPublisher = mock(NotificationPublisher.class);
        CashMetrics cashMetrics = mock(CashMetrics.class);
        when(accountsClient.withdraw("demo", 40))
                .thenReturn(new AccountDetailsDto("demo", "Ivan Ivanov", LocalDate.of(2001, 1, 1), 60));
        CashService cashService = new CashService(accountsClient, notificationPublisher, cashMetrics);

        var result = cashService.process("demo", new CashRequest(40, CashAction.GET));
        ArgumentCaptor<NotificationRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationRequest.class);

        assertThat(result.message()).isEqualTo("\u0421\u043D\u044F\u0442\u043E 40 \u0440\u0443\u0431.");
        verify(accountsClient).withdraw("demo", 40);
        verify(notificationPublisher).publish(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().type()).isEqualTo("CASH_WITHDRAW");
        assertThat(notificationCaptor.getValue().amount()).isEqualTo(40);
    }
}
