package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.client.NotificationClient;
import ru.yandex.practicum.mybank.accounts.error.BankException;
import ru.yandex.practicum.mybank.accounts.model.Account;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.AccountDto;
import ru.yandex.practicum.mybank.common.dto.AccountUpdateRequest;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;

import java.time.LocalDate;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final NotificationClient notificationClient;

    public AccountService(AccountRepository accountRepository, NotificationClient notificationClient) {
        this.accountRepository = accountRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional(readOnly = true)
    public AccountDetailsDto getCurrentAccount(String login) {
        return toDetails(getRequired(login));
    }

    @Transactional
    public AccountDetailsDto createIfMissing(String login) {
        Account account = accountRepository.findById(login)
                .orElseGet(() -> accountRepository.save(new Account(
                        login,
                        "Клиент " + login,
                        LocalDate.now().minusYears(18),
                        0
                )));
        return toDetails(account);
    }

    @Transactional
    public AccountDetailsDto updateCurrentAccount(String login, AccountUpdateRequest request) {
        Account account = getRequired(login);
        validateBirthdate(request.birthdate());
        account.setName(request.name().trim());
        account.setBirthdate(request.birthdate());
        notificationClient.notify(new NotificationRequest(
                login,
                "ACCOUNT_UPDATED",
                "Данные аккаунта обновлены",
                null
        ));
        return toDetails(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getTransferRecipients(String login) {
        return accountRepository.findByLoginNotOrderByNameAsc(login).stream()
                .map(account -> new AccountDto(account.getLogin(), account.getName()))
                .toList();
    }

    @Transactional
    public AccountDetailsDto deposit(String login, InternalCashRequest request) {
        Account account = getRequired(login);
        validateAmount(request.value());
        account.deposit(request.value());
        return toDetails(account);
    }

    @Transactional
    public AccountDetailsDto withdraw(String login, InternalCashRequest request) {
        Account account = getRequired(login);
        validateAmount(request.value());
        if (account.getBalance() < request.value()) {
            throw new BankException(HttpStatus.CONFLICT, "Недостаточно средств на счёте");
        }
        account.withdraw(request.value());
        return toDetails(account);
    }

    private Account getRequired(String login) {
        return accountRepository.findById(login)
                .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND, "Аккаунт %s не найден".formatted(login)));
    }

    private void validateAmount(long value) {
        if (value <= 0) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Сумма должна быть больше нуля");
        }
    }

    private void validateBirthdate(LocalDate birthdate) {
        if (birthdate == null) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Дата рождения обязательна");
        }
        if (birthdate.isAfter(LocalDate.now().minusYears(18))) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Возраст клиента должен быть старше 18 лет");
        }
    }

    private AccountDetailsDto toDetails(Account account) {
        return new AccountDetailsDto(
                account.getLogin(),
                account.getName(),
                account.getBirthdate(),
                account.getBalance()
        );
    }
}
