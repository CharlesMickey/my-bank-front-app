package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.common.kafka.NotificationPublisher;
import ru.yandex.practicum.mybank.transfer.client.AccountsClient;
import ru.yandex.practicum.mybank.transfer.service.TransferMetrics;
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
    void transferDelegatesAtomicTransferToAccountsService() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationPublisher notificationPublisher = mock(NotificationPublisher.class);
        TransferMetrics transferMetrics = mock(TransferMetrics.class);
        when(accountsClient.transfer("demo", "petrov", 25))
                .thenReturn(new AccountDetailsDto("demo", "Ivan Ivanov", LocalDate.of(2001, 1, 1), 75));
        TransferService service = new TransferService(accountsClient, notificationPublisher, transferMetrics);

        var result = service.transfer("demo", new TransferRequest("petrov", 25));
        ArgumentCaptor<NotificationRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationRequest.class);

        assertThat(result.message()).contains("25").contains("petrov");
        verify(accountsClient).transfer("demo", "petrov", 25);
        verify(notificationPublisher, times(2)).publish(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(NotificationRequest::type)
                .containsExactly("TRANSFER_SENT", "TRANSFER_RECEIVED");
    }

    @Test
    void transferDoesNotSendNotificationsWhenAccountsServiceFails() {
        AccountsClient accountsClient = mock(AccountsClient.class);
        NotificationPublisher notificationPublisher = mock(NotificationPublisher.class);
        TransferMetrics transferMetrics = mock(TransferMetrics.class);
        RuntimeException failure = new RuntimeException("transfer failed");
        when(accountsClient.transfer("demo", "petrov", 25)).thenThrow(failure);
        TransferService service = new TransferService(accountsClient, notificationPublisher, transferMetrics);

        assertThatThrownBy(() -> service.transfer("demo", new TransferRequest("petrov", 25)))
                .isSameAs(failure);

        verify(accountsClient).transfer("demo", "petrov", 25);
        verify(notificationPublisher, never()).publish(any());
    }
}
