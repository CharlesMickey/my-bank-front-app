package ru.yandex.practicum.mybank.cash.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mybank.cash.service.CashService;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;

@RestController
public class CashController {
    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @PostMapping("/api/cash")
    public OperationResultDto process(Authentication authentication, @Valid @RequestBody CashRequest request) {
        return cashService.process(authentication.getName(), request);
    }
}
