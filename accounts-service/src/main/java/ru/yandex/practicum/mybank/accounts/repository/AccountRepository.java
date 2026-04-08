package ru.yandex.practicum.mybank.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mybank.accounts.model.Account;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByLoginNotOrderByNameAsc(String login);
}
