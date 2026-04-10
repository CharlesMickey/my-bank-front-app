package ru.yandex.practicum.mybank.front.client;

import ru.yandex.practicum.mybank.common.dto.AccountDetailsDto;
import ru.yandex.practicum.mybank.common.dto.AccountDto;

import java.util.List;

public record AccountPage(AccountDetailsDto account, List<AccountDto> accounts) {
}
