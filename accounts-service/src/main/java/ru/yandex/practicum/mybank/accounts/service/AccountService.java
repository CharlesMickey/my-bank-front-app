package ru.yandex.practicum.mybank.accounts.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mybank.accounts.error.BankException;
import ru.yandex.practicum.mybank.accounts.model.Account;
import ru.yandex.practicum.mybank.accounts.repository.AccountRepository;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.AccountDto;
import ru.yandex.practicum.mybank.common.dto.AccountUpdateRequest;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;
import ru.yandex.practicum.mybank.common.dto.InternalTransferRequest;
import ru.yandex.practicum.mybank.common.dto.NotificationRequest;
import ru.yandex.practicum.mybank.common.kafka.NotificationPublisher;

import java.time.LocalDate;
import java.util.List;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final NotificationPublisher notificationPublisher;
    private final ObservationRegistry observationRegistry;

    public AccountService(
            AccountRepository accountRepository,
            NotificationPublisher notificationPublisher,
            ObservationRegistry observationRegistry
    ) {
        this.accountRepository = accountRepository;
        this.notificationPublisher = notificationPublisher;
        this.observationRegistry = observationRegistry;
    }

    @Transactional(readOnly = true)
    public AccountDetailsDto getCurrentAccount(String login) {
        log.info("Loading account details for login={}", login);
        return toDetails(getRequired(login));
    }

    @Transactional
    public AccountDetailsDto createIfMissing(String login) {
        Account account = observeDatabase("accounts-service db load-or-create account", () -> accountRepository.findById(login)
                .orElseGet(() -> observeDatabase("accounts-service db create account", () -> accountRepository.save(new Account(
                        login,
                        "\u041A\u043B\u0438\u0435\u043D\u0442 " + login,
                        LocalDate.now().minusYears(18),
                        0
                )))));
        if ("\u041A\u043B\u0438\u0435\u043D\u0442 ".concat(login).equals(account.getName()) && account.getBalance() == 0) {
            log.info("Account ensured for login={}", login);
        }
        return toDetails(account);
    }

    @Transactional
    public AccountDetailsDto updateCurrentAccount(String login, AccountUpdateRequest request) {
        Account account = getRequired(login);
        validateBirthdate(request.birthdate());
        account.setName(request.name().trim());
        account.setBirthdate(request.birthdate());
        notificationPublisher.publish(new NotificationRequest(
                login,
                "ACCOUNT_UPDATED",
                "\u0414\u0430\u043D\u043D\u044B\u0435 \u0430\u043A\u043A\u0430\u0443\u043D\u0442\u0430 \u043E\u0431\u043D\u043E\u0432\u043B\u0435\u043D\u044B",
                null
        ));
        log.info("Account updated for login={}", login);
        return toDetails(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getTransferRecipients(String login) {
        List<AccountDto> recipients = observeDatabase("accounts-service db load recipients", () -> accountRepository.findByLoginNotOrderByNameAsc(login).stream()
                .map(account -> new AccountDto(account.getLogin(), account.getName()))
                .toList());
        log.info("Loaded transfer recipients for login={} count={}", login, recipients.size());
        return recipients;
    }

    @Transactional
    public AccountDetailsDto deposit(String login, InternalCashRequest request) {
        Account account = getRequired(login);
        validateAmount(request.value());
        account.deposit(request.value());
        log.info("Internal deposit completed for login={} amount={}", login, request.value());
        return toDetails(account);
    }

    @Transactional
    public AccountDetailsDto withdraw(String login, InternalCashRequest request) {
        Account account = getRequired(login);
        validateAmount(request.value());
        if (account.getBalance() < request.value()) {
            throw new BankException(HttpStatus.CONFLICT,
                    "\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043D\u0430 \u0441\u0447\u0451\u0442\u0435");
        }
        account.withdraw(request.value());
        log.info("Internal withdrawal completed for login={} amount={}", login, request.value());
        return toDetails(account);
    }

    @Transactional
    public AccountDetailsDto transfer(String login, InternalTransferRequest request) {
        validateAmount(request.value());
        if (login.equals(request.recipientLogin())) {
            throw new BankException(HttpStatus.BAD_REQUEST,
                    "\u041D\u0435\u043B\u044C\u0437\u044F \u043F\u0435\u0440\u0435\u0432\u0435\u0441\u0442\u0438 \u0434\u0435\u043D\u044C\u0433\u0438 \u0441\u0430\u043C\u043E\u043C\u0443 \u0441\u0435\u0431\u0435");
        }

        Account sender = getRequired(login);
        Account recipient = getRequired(request.recipientLogin());
        if (sender.getBalance() < request.value()) {
            throw new BankException(HttpStatus.CONFLICT,
                    "\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u0441\u0440\u0435\u0434\u0441\u0442\u0432 \u043D\u0430 \u0441\u0447\u0451\u0442\u0435");
        }

        sender.withdraw(request.value());
        recipient.deposit(request.value());
        log.info("Internal transfer completed senderLogin={} recipientLogin={} amount={}", login, request.recipientLogin(), request.value());
        return toDetails(sender);
    }

    private Account getRequired(String login) {
        return observeDatabase("accounts-service db load account", () -> accountRepository.findById(login)
                .orElseThrow(() -> new BankException(HttpStatus.NOT_FOUND,
                        "\u0410\u043A\u043A\u0430\u0443\u043D\u0442 %s \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D".formatted(login))));
    }

    private void validateAmount(long value) {
        if (value <= 0) {
            throw new BankException(HttpStatus.BAD_REQUEST,
                    "\u0421\u0443\u043C\u043C\u0430 \u0434\u043E\u043B\u0436\u043D\u0430 \u0431\u044B\u0442\u044C \u0431\u043E\u043B\u044C\u0448\u0435 \u043D\u0443\u043B\u044F");
        }
    }

    private void validateBirthdate(LocalDate birthdate) {
        if (birthdate == null) {
            throw new BankException(HttpStatus.BAD_REQUEST,
                    "\u0414\u0430\u0442\u0430 \u0440\u043E\u0436\u0434\u0435\u043D\u0438\u044F \u043E\u0431\u044F\u0437\u0430\u0442\u0435\u043B\u044C\u043D\u0430");
        }
        if (birthdate.isAfter(LocalDate.now().minusYears(18))) {
            throw new BankException(HttpStatus.BAD_REQUEST,
                    "\u0412\u043E\u0437\u0440\u0430\u0441\u0442 \u043A\u043B\u0438\u0435\u043D\u0442\u0430 \u0434\u043E\u043B\u0436\u0435\u043D \u0431\u044B\u0442\u044C \u0441\u0442\u0430\u0440\u0448\u0435 18 \u043B\u0435\u0442");
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

    private <T> T observeDatabase(String contextualName, java.util.function.Supplier<T> operation) {
        return Observation.createNotStarted("bank.db.operation", observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("db.system", "postgresql")
                .lowCardinalityKeyValue("service", "accounts-service")
                .observe(operation);
    }
}
