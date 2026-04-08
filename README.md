# My Bank App

Микросервисное приложение «Банк Бабанк» для проектной работы девятого спринта. Проект собран как Maven multi-module на Java 21, Spring Boot и Spring Cloud.

## Архитектура

| Модуль | Назначение | Порт |
| --- | --- | --- |
| `front-ui` | Thymeleaf UI, Authorization Code Flow, запросы в backend только через Gateway | `8080` |
| `gateway-service` | Spring Cloud Gateway, проверка JWT и маршрутизация в сервисы через Eureka | `8090` |
| `accounts-service` | Владение аккаунтами, датой рождения и балансом, PostgreSQL schema `accounts` | `8081` |
| `cash-service` | Пополнение и снятие денег, межсервисный Client Credentials Flow | `8082` |
| `transfer-service` | Переводы между аккаунтами, компенсация при сбое зачисления | `8083` |
| `notifications-service` | Уведомления в лог и PostgreSQL schema `notifications` | `8084` |
| `discovery-server` | Eureka Service Discovery | `8761` |
| `config-server` | Spring Cloud Config Server, native config | `8888` |
| `bank-common` | Общие DTO, доменные ошибки, JWT authority converter | - |

## Security

Используется Keycloak realm `my-bank`.

Пользовательский вход во фронт работает по Authorization Code Flow. Фронт получает access token пользователя и передаёт его в `gateway-service`, который дальше пробрасывает `Authorization` header в backend-сервисы.

Межсервисные вызовы `accounts-service`, `cash-service` и `transfer-service` выполняются по Client Credentials Flow. Роли Keycloak из `realm_access.roles` конвертируются в authorities вида `SCOPE_accounts.read`, `SCOPE_accounts.write`, `SCOPE_cash.write`, `SCOPE_transfer.write`, `SCOPE_notifications.write`.

Демо-пользователи Keycloak:

| Login | Password |
| --- | --- |
| `demo` | `password` |
| `petrov` | `password` |
| `sidorov` | `password` |

## Сборка и тесты

```bash
./mvnw clean package
```

На Windows можно использовать:

```powershell
.\mvnw.cmd clean package
```

Запуск только тестов:

```bash
./mvnw test
```

В тестах используется JUnit 5, Spring Boot Test, кеширование контекстов Spring TestContext и H2 для JPA-сервисов. Для `accounts-service` добавлен провайдерский контракт Spring Cloud Contract на `GET /api/accounts/me`. Runtime-схемы PostgreSQL накатываются Flyway.

## Запуск в Docker Compose

```bash
docker compose up --build
```

После старта откройте:

```text
http://localhost:8080
```

Полезные адреса:

| Сервис | URL |
| --- | --- |
| Front UI | `http://localhost:8080` |
| Gateway API | `http://localhost:8090` |
| Eureka | `http://localhost:8761` |
| Config Server | `http://localhost:8888/application/default` |
| Keycloak Admin | `http://localhost:9090` |

Keycloak admin: `admin` / `admin`.

## Локальный запуск без Docker

1. Поднимите PostgreSQL с базой `bank`, пользователем `bank`, паролем `bank`.
2. Поднимите Keycloak на `http://localhost:9090` и импортируйте `keycloak/my-bank-realm.json`.
3. Запустите сервисы в таком порядке: `discovery-server`, `config-server`, `accounts-service`, `notifications-service`, `cash-service`, `transfer-service`, `gateway-service`, `front-ui`.
4. Откройте `http://localhost:8080`.

## Бизнес-функции

- Просмотр и редактирование ФИО и даты рождения аккаунта.
- Валидация обязательных полей и возраста старше 18 лет.
- Пополнение и снятие виртуальных рублей.
- Ошибка при снятии суммы больше текущего баланса.
- Перевод другому клиенту с проверкой баланса отправителя.
- Уведомления о действиях в лог и таблицу `notifications.events`.

## Данные

`accounts-service` создаёт демо-аккаунты при старте:

| Login | Name | Balance |
| --- | --- | --- |
| `demo` | `Иванов Иван` | `100` |
| `petrov` | `Петров Пётр` | `250` |
| `sidorov` | `Сидоров Сидор` | `300` |

Для Database per Service используется одна PostgreSQL база `bank` с отдельными схемами:

| Сервис | Schema |
| --- | --- |
| `accounts-service` | `accounts` |
| `notifications-service` | `notifications` |

`cash-service` и `transfer-service` не владеют постоянными данными.
