package ru.yandex.practicum.mybank.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import ru.yandex.practicum.mybank.accounts.error.BankException;
import ru.yandex.practicum.mybank.accounts.service.AccountService;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;
import ru.yandex.practicum.mybank.common.dto.InternalTransferRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:accounts-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE SCHEMA IF NOT EXISTS accounts",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AccountServiceTest {
    @Autowired
    private AccountService accountService;

    @Test
    void depositIncreasesBalance() {
        long before = accountService.getCurrentAccount("demo").sum();

        long after = accountService.deposit("demo", new InternalCashRequest(50)).sum();

        assertThat(after).isEqualTo(before + 50);
    }

    @Test
    void withdrawRejectsTooLargeAmount() {
        assertThatThrownBy(() -> accountService.withdraw("demo", new InternalCashRequest(1_000_000)))
                .isInstanceOfSatisfying(BankException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void transferMovesMoneyBetweenAccountsAtomically() {
        long senderBefore = accountService.getCurrentAccount("demo").sum();
        long recipientBefore = accountService.getCurrentAccount("petrov").sum();

        long senderAfter = accountService.transfer("demo", new InternalTransferRequest("petrov", 25)).sum();
        long recipientAfter = accountService.getCurrentAccount("petrov").sum();

        assertThat(senderAfter).isEqualTo(senderBefore - 25);
        assertThat(recipientAfter).isEqualTo(recipientBefore + 25);
    }

    @Test
    void failedTransferDoesNotChangeBalances() {
        long senderBefore = accountService.getCurrentAccount("demo").sum();
        long recipientBefore = accountService.getCurrentAccount("petrov").sum();

        assertThatThrownBy(() -> accountService.transfer("demo", new InternalTransferRequest("petrov", senderBefore + 1)))
                .isInstanceOfSatisfying(BankException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(accountService.getCurrentAccount("demo").sum()).isEqualTo(senderBefore);
        assertThat(accountService.getCurrentAccount("petrov").sum()).isEqualTo(recipientBefore);
    }
}
