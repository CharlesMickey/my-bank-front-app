package ru.yandex.practicum.mybank.accounts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;

@Entity
@Table(name = "accounts", schema = "accounts")
public class Account {
    @Id
    @Column(nullable = false, length = 80)
    private String login;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthdate;

    @Column(nullable = false)
    private long balance;

    @Version
    private long version;

    protected Account() {
    }

    public Account(String login, String name, LocalDate birthdate, long balance) {
        this.login = login;
        this.name = name;
        this.birthdate = birthdate;
        this.balance = balance;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public long getBalance() {
        return balance;
    }

    public void deposit(long value) {
        balance += value;
    }

    public void withdraw(long value) {
        balance -= value;
    }
}
