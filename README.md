# My Bank App

Микросервисное приложение «Банк Бабанк» для проектной работы десятого спринта. Проект собран как Maven multi-module на Java 21, Spring Boot и Spring Cloud. Для развёртывания в Kubernetes используется зонтичный Helm-чарт с сабчартами для микросервисов, PostgreSQL и Keycloak.

## Выбранная схема развёртывания

- `front-ui` запускается вне Kubernetes локально.
- `gateway-service`, `accounts-service`, `cash-service`, `transfer-service`, `notifications-service`, `Keycloak` и PostgreSQL-базы запускаются в Kubernetes.
- Внутри кластера Service Discovery реализован штатно через Kubernetes `Service` и DNS-имена сервисов.
- Внутри кластера Externalized Config реализован через `ConfigMap` и `Secret`.
- В качестве Gateway API сохранён `gateway-service`.
- Для доступа снаружи кластера используются `NodePort`:
  - Gateway: `30080`
  - Keycloak: `30090`

## Активные модули

| Модуль | Назначение | Порт |
| --- | --- | --- |
| `front-ui` | Thymeleaf UI, Authorization Code Flow, запросы в backend только через Gateway | `8080` |
| `gateway-service` | Gateway API, проверка JWT и маршрутизация запросов | `8090` |
| `accounts-service` | Аккаунты, баланс, дата рождения, PostgreSQL | `8081` |
| `cash-service` | Пополнение и снятие денег | `8082` |
| `transfer-service` | Переводы между аккаунтами | `8083` |
| `notifications-service` | Уведомления и журнал событий, PostgreSQL | `8084` |
| `bank-common-dto` | Общие DTO | - |
| `bank-common-security` | Общая security-логика | - |
| `bank-common-oauth2-client` | Общая OAuth2 client-конфигурация | - |

`discovery-server` и `config-server` сохранены в репозитории как наследие девятого спринта, но не входят в активную Maven-сборку и не используются в Kubernetes-развёртывании.

## Kubernetes и Helm

Структура Helm-чарта:

- зонтичный чарт: `helm/my-bank`
- сабчарты:
  - `gateway-service`
  - `accounts-service`
  - `cash-service`
  - `transfer-service`
  - `notifications-service`
  - `accounts-postgres`
  - `notifications-postgres`
  - `keycloak`

Для баз данных используются `StatefulSet`, для приложений и Keycloak используются `Deployment`.

## Требования

- Java 21
- Maven 3.9+ или Maven Wrapper
- Docker
- локальный Kubernetes-кластер: Kind, Minikube, Rancher Desktop, Docker Desktop Kubernetes или аналог
- `kubectl`
- `helm`

## Сборка и тесты

Сборка проекта:

```powershell
.\mvnw.cmd clean package
```

Запуск тестов:

```powershell
.\mvnw.cmd test
```

В проекте используются JUnit 5, Spring Boot Test, Spring Cloud Contract и H2 для интеграционных тестов сервисов.

## Сборка Docker-образов

Перед установкой Helm-чарта нужно собрать образы сервисов:

```powershell
$services = 'gateway-service','accounts-service','cash-service','transfer-service','notifications-service'
foreach ($service in $services) {
  docker build -f "$service/Dockerfile" -t "${service}:latest" .
}
```

Если ваш Kubernetes использует тот же Docker daemon, дополнительная загрузка образов не нужна. Для `kind` и `minikube` загрузите образы вручную.

Пример для `kind`:

```powershell
$services = 'gateway-service','accounts-service','cash-service','transfer-service','notifications-service'
foreach ($service in $services) {
  kind load docker-image "${service}:latest"
}
```

Пример для `minikube`:

```powershell
$services = 'gateway-service','accounts-service','cash-service','transfer-service','notifications-service'
foreach ($service in $services) {
  minikube image load "${service}:latest"
}
```

## Установка в Kubernetes

Обновите зависимости чарта и установите приложение:

```powershell
helm dependency update .\helm\my-bank
helm upgrade --install my-bank .\helm\my-bank --namespace my-bank --create-namespace
```

Проверка ресурсов:

```powershell
kubectl get pods -n my-bank
kubectl get svc -n my-bank
```

Запуск Helm hook tests:

```powershell
helm test my-bank --namespace my-bank
```

## Доступ к приложению

После установки чарта точки входа будут такими:

- Gateway API: `http://localhost:30080`
- Keycloak: `http://localhost:30090`

Администратор Keycloak:

- логин: `admin`
- пароль: `admin`

Демо-пользователи:

| Login | Password |
| --- | --- |
| `demo` | `password` |
| `petrov` | `password` |
| `sidorov` | `password` |

## Запуск Front UI вне Kubernetes

Фронт запускается локально и обращается к Gateway и Keycloak в Kubernetes через `NodePort`.

PowerShell:

```powershell
$env:BANK_GATEWAY_URL='http://localhost:30080'
$env:BANK_AUTHORIZATION_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/auth'
$env:BANK_TOKEN_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/token'
$env:BANK_USER_INFO_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/userinfo'
$env:BANK_JWK_SET_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/certs'
$env:BANK_LOGOUT_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/logout'
$env:FRONT_CLIENT_ID='front-ui'
$env:FRONT_CLIENT_SECRET='front-secret'
.\mvnw.cmd -pl front-ui spring-boot:run
```

После старта откройте:

```text
http://localhost:8080
```

## Что хранится в Kubernetes

- `ConfigMap`:
  - адреса межсервисного взаимодействия
  - порты приложений
  - OAuth2 endpoints
  - параметры PostgreSQL и Keycloak
- `Secret`:
  - пароли PostgreSQL
  - client secret сервисов
  - пароль администратора Keycloak

## Базы данных

Используются отдельные PostgreSQL StatefulSet:

| Сабчарт | База данных | Используется сервисом |
| --- | --- | --- |
| `accounts-postgres` | `accounts` | `accounts-service` |
| `notifications-postgres` | `notifications` | `notifications-service` |

Схемы создаются Flyway при старте приложений:

- `accounts-service` использует схему `accounts`
- `notifications-service` использует схему `notifications`

## Helm-тесты

В сабчартах реализованы Helm hook tests:

- проверка доступности `gateway-service`
- проверка доступности `accounts-service`
- проверка доступности `cash-service`
- проверка доступности `transfer-service`
- проверка доступности `notifications-service`
- проверка доступности `keycloak`
- проверка доступности `accounts-postgres`
- проверка доступности `notifications-postgres`

## Полезные команды

Обновить релиз после изменения values или шаблонов:

```powershell
helm upgrade my-bank .\helm\my-bank --namespace my-bank
```

Удалить релиз:

```powershell
helm uninstall my-bank --namespace my-bank
```
