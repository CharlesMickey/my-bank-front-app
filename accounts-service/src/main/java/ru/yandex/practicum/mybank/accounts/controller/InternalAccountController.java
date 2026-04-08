package ru.yandex.practicum.mybank.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.accounts.service.AccountService;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.InternalCashRequest;

@RestController
@RequestMapping("/api/internal/accounts")
public class InternalAccountController {
    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{login}/deposit")
    public AccountDetailsDto deposit(@PathVariable String login, @Valid @RequestBody InternalCashRequest request) {
        return accountService.deposit(login, request);
    }

    @PostMapping("/{login}/withdraw")
    public AccountDetailsDto withdraw(@PathVariable String login, @Valid @RequestBody InternalCashRequest request) {
        return accountService.withdraw(login, request);
    }
}
