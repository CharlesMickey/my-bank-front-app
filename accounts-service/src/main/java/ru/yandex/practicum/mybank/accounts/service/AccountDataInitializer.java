package ru.yandex.practicum.mybank.accounts.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mybank.accounts.model.Account;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;

import java.time.LocalDate;
import java.util.List;

@Component
public class AccountDataInitializer implements ApplicationRunner {
    private final AccountRepository accountRepository;

    public AccountDataInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Account> demoAccounts = List.of(
                new Account("demo", "Иванов Иван", LocalDate.of(2001, 1, 1), 100),
                new Account("petrov", "Петров Пётр", LocalDate.of(1997, 5, 20), 250),
                new Account("sidorov", "Сидоров Сидор", LocalDate.of(1994, 10, 12), 300)
        );
        demoAccounts.stream()
                .filter(account -> !accountRepository.existsById(account.getLogin()))
                .forEach(accountRepository::save);
    }
}
