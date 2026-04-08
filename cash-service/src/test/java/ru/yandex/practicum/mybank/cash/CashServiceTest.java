package ru.yandex.practicum.mybank.cash;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mybank.cash.client.AccountsClient;
import ru.yandex.practicum.mybank.cash.client.NotificationClient;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.CashAction;
import ru.yandex.practicum.mybank.common.dto.CashRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CashServiceTest {

    @Test
    void depositCallsAccountsServiceAndNotificationService() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationClient notificationClient = mock(NotificationClient.class);
        when(accountsClient.deposit("demo", 50))
                .thenReturn(new AccountDetailsDto("demo", "Иванов Иван", LocalDate.of(2001, 1, 1), 150));
        CashService cashService = new CashService(accountsClient, notificationClient);

        var result = cashService.process("demo", new CashRequest(50, CashAction.PUT));

        assertThat(result.message()).isEqualTo("Положено 50 руб.");
        verify(accountsClient).deposit("demo", 50);
    }
}
