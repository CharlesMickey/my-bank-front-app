package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.client.NotificationClient;
import ru.yandex.practicum.mybank.transfer.service.TransferService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        ArgumentCaptor<NotificationRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationRequest.class);

        assertThat(result.message()).contains("25 руб.").contains("petrov");
        verify(accountsClient).withdraw("demo", 25);
        verify(accountsClient).deposit("petrov", 25);
        verify(notificationClient, times(2)).notify(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(NotificationRequest::type)
                .containsExactly("TRANSFER_SENT", "TRANSFER_RECEIVED");
    }

    @Test
    void transferRollsBackWhenDepositToRecipientFails() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationClient notificationClient = mock(NotificationClient.class);
        RuntimeException failure = new RuntimeException("recipient deposit failed");
        when(accountsClient.withdraw("demo", 25))
                .thenReturn(new AccountDetailsDto("demo", "Иванов Иван", LocalDate.of(2001, 1, 1), 75));
        when(accountsClient.deposit("petrov", 25)).thenThrow(failure);
        when(accountsClient.deposit("demo", 25))
                .thenReturn(new AccountDetailsDto("demo", "Иванов Иван", LocalDate.of(2001, 1, 1), 100));
        TransferService service = new TransferService(accountsClient, notificationClient);

        assertThatThrownBy(() -> service.transfer("demo", new TransferRequest("petrov", 25)))
                .isSameAs(failure);

        verify(accountsClient).withdraw("demo", 25);
        verify(accountsClient).deposit("petrov", 25);
        verify(accountsClient).deposit("demo", 25);
        verify(notificationClient, never()).notify(any());
    }
}
