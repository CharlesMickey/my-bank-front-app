package ru.yandex.practicum.mybank.accounts.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.accounts.service.AccountService;
import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.AccountDto;
import ru.yandex.practicum.mybank.common.dto.AccountUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountDetailsDto getCurrentAccount(Authentication authentication) {
        accountService.createIfMissing(authentication.getName());
        return accountService.getCurrentAccount(authentication.getName());
    }

    @PutMapping("/me")
    public AccountDetailsDto updateCurrentAccount(Authentication authentication,
                                                  @Valid @RequestBody AccountUpdateRequest request) {
        accountService.createIfMissing(authentication.getName());
        return accountService.updateCurrentAccount(authentication.getName(), request);
    }

    @GetMapping("/recipients")
    public List<AccountDto> getTransferRecipients(Authentication authentication) {
        accountService.createIfMissing(authentication.getName());
        return accountService.getTransferRecipients(authentication.getName());
    }
}
