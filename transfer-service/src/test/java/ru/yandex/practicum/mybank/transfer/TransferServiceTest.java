package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.NotificationClient;
import ru.yandex.practicum.mybank.transfer.service.TransferService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceTest {

    @Test
    void transferWithdrawsFromSenderAndDepositsToRecipient() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationClient notificationClient = mock(NotificationClient.class);
        when(accountsClient.withdraw("demo", 25))
                .thenReturn(new AccountDetailsDto("demo", "Иванов Иван", LocalDate.of(2001, 1, 1), 75));
        when(accountsClient.deposit("petrov", 25))
                .thenReturn(new AccountDetailsDto("petrov", "Петров Пётр", LocalDate.of(1997, 5, 20), 275));
        TransferService service = new TransferService(accountsClient, notificationClient);

        var result = service.transfer("demo", new TransferRequest("petrov", 25));

        assertThat(result.message()).contains("25 руб.").contains("petrov");
        verify(accountsClient).withdraw("demo", 25);
        verify(accountsClient).deposit("petrov", 25);
    }
}
