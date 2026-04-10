package ru.yandex.practicum.mybank.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "events", schema = "notifications")
public class NotificationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String login;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    private Long amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEvent() {
    }

    public NotificationEvent(String login, String type, String message, Long amount, Instant createdAt) {
        this.login = login;
        this.type = type;
        this.message = message;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Long getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
