# My Bank App

Микросервисное приложение «Банк» для проектной работы одиннадцатого спринта. Проект собран как Maven multi-module на Java 21, Spring Boot и Spring Cloud. Для уведомлений используется Apache Kafka, а для развёртывания в Kubernetes используется зонтичный Helm-чарт с сабчартами для сервисов, PostgreSQL, Kafka и Keycloak.

## Выбранная схема развёртывания

- `front-ui` запускается локально вне Kubernetes.
- `gateway-service`, `accounts-service`, `cash-service`, `transfer-service`, `notifications-service`, `keycloak`, PostgreSQL и Kafka запускаются в Kubernetes.
- Внутри кластера Service Discovery реализован штатно через Kubernetes `Service` и DNS-имена сервисов.
- Конфигурация сервисов вынесена в `ConfigMap` и `Secret`.
- В качестве Gateway API сохранён `gateway-service`.
- Уведомления передаются только через Apache Kafka, без REST-вызовов в `notifications-service`.
- Для доступа снаружи кластера используются `NodePort`:
  - Gateway: `30080`
  - Keycloak: `30090`

## Активные модули

| Модуль | Назначение | Порт |
| --- | --- | --- |
| `front-ui` | Thymeleaf UI, Authorization Code Flow, запросы в backend только через Gateway | `8080` |
| `gateway-service` | Gateway API, проверка JWT и маршрутизация запросов | `8090` |
| `accounts-service` | Аккаунты, баланс, редактирование профиля, PostgreSQL | `8081` |
| `cash-service` | Пополнение и снятие денег | `8082` |
| `transfer-service` | Переводы между аккаунтами | `8083` |
| `notifications-service` | Kafka consumer, журнал уведомлений, PostgreSQL | без HTTP-порта |
| `bank-common-dto` | Общие DTO | - |
| `bank-common-security` | Общая security-логика | - |
| `bank-common-oauth2-client` | Общая OAuth2 client-конфигурация | - |
| `bank-common-kafka` | Общая Kafka-конфигурация и publisher уведомлений | - |

## Схема уведомлений через Kafka

- `accounts-service`, `cash-service` и `transfer-service` публикуют события в Kafka topic `bank.notifications`.
- Публикация настроена со стратегией `at least once`:
  - producer использует `acks=all`
  - включены `retries`
  - включена идемпотентность producer
- `notifications-service` читает события через consumer group `notifications-service`.
- Смещение подтверждается после обработки сообщения, поэтому после рестарта сервис продолжает чтение с последнего подтверждённого сообщения.
- Порядок сообщений между разными типами уведомлений специально не гарантируется.

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
  - `kafka`
  - `keycloak`

Для баз данных и Kafka используются `StatefulSet`, для приложений и Keycloak используются `Deployment`.

## Требования

- Java 21
- Maven 3.9+ или Maven Wrapper
- Docker Desktop
- локальный Kubernetes-кластер: Docker Desktop Kubernetes, Kind, Minikube, Rancher Desktop или аналог
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

В проекте используются:

- JUnit 5
- Spring Boot Test
- Spring Cloud Contract для `accounts-service`
- Embedded Kafka для интеграционных тестов взаимодействия через Kafka
- H2 для интеграционных тестов сервисов

## Локальный запуск через Docker Compose

Собрать и запустить все контейнеры:

```powershell
docker compose up --build -d
```

Проверить состояние:

```powershell
docker compose ps
```

Что поднимается в Compose:

- `postgres`
- `kafka`
- `kafka-init` для создания topic `bank.notifications`
- `keycloak`
- `gateway-service`
- `front-ui`
- `accounts-service`
- `cash-service`
- `transfer-service`
- `notifications-service`

Точки входа при запуске через Compose:

- Front UI: `http://localhost:8080`
- Gateway: `http://localhost:8090`
- Keycloak: `http://localhost:9090`
- Kafka broker: `localhost:9092`

## Сборка Docker-образов для Kubernetes

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

Обновить зависимости чарта и установить приложение:

```powershell
helm dependency update .\helm\my-bank
helm upgrade --install my-bank .\helm\my-bank --namespace my-bank --create-namespace
```

Проверить ресурсы:

```powershell
kubectl get deployments,sts,svc -n my-bank
kubectl get pods -n my-bank
```

Проверить Helm chart:

```powershell
helm lint .\helm\my-bank
helm test my-bank --namespace my-bank
```

## Доступ к приложению в Kubernetes

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
  - OAuth2 endpoints
  - параметры Kafka и topic name
  - параметры PostgreSQL
- `Secret`:
  - пароли PostgreSQL
  - client secret сервисов
  - пароль администратора Keycloak

## Базы данных и Kafka

Используются отдельные persistent StatefulSet:

| Сабчарт | Назначение | Используется сервисом |
| --- | --- | --- |
| `accounts-postgres` | PostgreSQL база `accounts` | `accounts-service` |
| `notifications-postgres` | PostgreSQL база `notifications` | `notifications-service` |
| `kafka` | Kafka broker в режиме KRaft | все producer/consumer сервисы |

Flyway-схемы создаются при старте приложений:

- `accounts-service` использует схему `accounts`
- `notifications-service` использует схему `notifications`

Kafka topic `bank.notifications` создаётся Helm hook job в Kubernetes и `kafka-init` сервисом в Docker Compose.

## Helm-тесты

В сабчартах реализованы Helm hook tests для проверки доступности:

- `gateway-service`
- `accounts-service`
- `cash-service`
- `transfer-service`
- `notifications-service`
- `keycloak`
- `accounts-postgres`
- `notifications-postgres`
- `kafka`

## Полезные команды

Обновить релиз после изменения values или шаблонов:

```powershell
helm upgrade my-bank .\helm\my-bank --namespace my-bank
```

Посмотреть итоговые YAML без установки:

```powershell
helm template my-bank .\helm\my-bank
```

Удалить релиз:

```powershell
helm uninstall my-bank --namespace my-bank
```
