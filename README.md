````markdown
# My Bank App

Микросервисное приложение «Банк» для проектной работы двенадцатого спринта.

Бизнес-схема приложения сохранена: `front-ui` работает вне Kubernetes, а `gateway-service`, `accounts-service`, `cash-service`, `transfer-service`, `notifications-service`, `keycloak`, PostgreSQL, Kafka и весь observability-стек запускаются в Kubernetes через зонтичный Helm-чарт.

## Что добавлено в observability

В проект добавелны:

- распределённая трассировка через Zipkin;
- метрики через Spring Boot Actuator, Micrometer и Prometheus;
- дашборд Grafana с HTTP, JVM и бизнес-метриками;
- JSON-логирование через Logback;
- отправка логов в Logstash;
- Elasticsearch и Kibana для поиска и анализа логов;
- Prometheus alerts для HTTP 5xx, latency, failed withdrawals, failed transfers, failed notifications и недоступных scrape-targets.

## Выбранная схема запуска

- `front-ui` запускается локально на `http://localhost:8080`;
- backend-сервисы и observability-стек запускаются в Kubernetes namespace `my-bank`;
- доступ к сервисам снаружи организован через NodePort.

## Требования

Для запуска проекта Вам потребуется:

- Java 21;
- Maven 3.9+ или Maven Wrapper;
- Docker Desktop с включённым Kubernetes;
- kubectl;
- Helm.

## Сборка и тесты

Собрать проект:

```powershell
.\mvnw.cmd clean package
````

Запустить тесты:

```powershell
.\mvnw.cmd test
```

## Docker-образы backend-сервисов

Перед установкой Helm-чарта собрать локальные Docker-образы сервисов:

```powershell
$services = 'gateway-service','accounts-service','cash-service','transfer-service','notifications-service'
foreach ($service in $services) {
  docker build -f "$service/Dockerfile" -t "${service}:latest" .
}
```

## Установка в Kubernetes

Обновить зависимости Helm-чарта:

```powershell
helm dependency update .\helm\my-bank
```

Установить или обновить релиз:

```powershell
helm upgrade --install my-bank .\helm\my-bank --namespace my-bank --create-namespace
```

Проверить ресурсы в namespace `my-bank`:

```powershell
kubectl get deployments,sts,svc -n my-bank
kubectl get pods -n my-bank
```

Проверить Helm-шаблоны и Helm-тесты:

```powershell
helm lint .\helm\my-bank
helm template my-bank .\helm\my-bank
helm test my-bank --namespace my-bank
```

## Доступные точки входа

| Компонент                                    | URL                      |
| -------------------------------------------- | ------------------------ |
| Front UI                                     | `http://localhost:8080`  |
| Gateway API                                  | `http://localhost:30080` |
| Keycloak                                     | `http://localhost:30090` |
| Prometheus                                   | `http://localhost:30091` |
| Kibana                                       | `http://localhost:30092` |
| Grafana                                      | `http://localhost:30093` |
| Zipkin                                       | `http://localhost:30094` |
| Logstash TCP input для локального `front-ui` | `localhost:30095`        |

## Запуск Front UI с observability

Для полной трассировки, метрик и логов запустить `front-ui` локально после установки Helm-чарта:

```powershell
$env:BANK_GATEWAY_URL='http://localhost:30080'
$env:BANK_AUTHORIZATION_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/auth'
$env:BANK_TOKEN_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/token'
$env:BANK_USER_INFO_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/userinfo'
$env:BANK_JWK_SET_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/certs'
$env:BANK_LOGOUT_URI='http://localhost:30090/realms/my-bank/protocol/openid-connect/logout'
$env:BANK_ZIPKIN_ENDPOINT='http://localhost:30094/api/v2/spans'
$env:BANK_LOGSTASH_HOST='localhost'
$env:BANK_LOGSTASH_PORT='30095'
$env:FRONT_CLIENT_ID='front-ui'
$env:FRONT_CLIENT_SECRET='front-secret'
.\mvnw.cmd -pl front-ui spring-boot:run
```

Prometheus в Kubernetes scrapes `front-ui` по адресу `host.docker.internal:8080`, поэтому для фронтовых метрик `front-ui` должен быть запущен локально.

Если Ваш Kubernetes-кластер не видит `host.docker.internal`, переопределить `prometheus.config.frontUiTarget` в Helm values на доступный адрес хоста.

## Пользователи для проверки

| Login     | Password   |
| --------- | ---------- |
| `demo`    | `password` |
| `petrov`  | `password` |
| `sidorov` | `password` |

Администратор Keycloak:

```text
login: admin
password: admin
```

## Actuator endpoints

Публично используются только следующие endpoints:

```text
/actuator/health
/actuator/info
/actuator/prometheus
```

Чувствительные actuator endpoints наружу не открываются.

## Zipkin и трассировка

Во всех HTTP-сервисах и `front-ui` включены:

* `micrometer-tracing-bridge-brave`;
* `zipkin-reporter-brave`;
* sampling probability `1.0`.

Для Kafka включено observation на producer/consumer.

Для сервисов с БД добавлены дочерние observation spans на операции доступа к данным.

Открыть Zipkin:

```text
http://localhost:30094
```

Проверить end-to-end trace:

1. Запустить Helm-чарт.
2. Запустить локально `front-ui` с `BANK_ZIPKIN_ENDPOINT=http://localhost:30094/api/v2/spans`.
3. Войти в приложение.
4. Выполнить одно из действий:

    * сохранить профиль;
    * пополнить счёт;
    * снять деньги;
    * перевести деньги.
5. Открыть Zipkin.
6. Найти трейсы по сервисам:

    * `front-ui`;
    * `gateway-service`;
    * `accounts-service`;
    * `cash-service`;
    * `transfer-service`;
    * `notifications-service`.

Ожидаемые цепочки трассировки:

```text
front-ui -> gateway-service -> accounts-service
front-ui -> gateway-service -> cash-service -> accounts-service -> Kafka -> notifications-service
front-ui -> gateway-service -> transfer-service -> accounts-service -> Kafka -> notifications-service
```

## Prometheus и метрики

Открыть Prometheus:

```text
http://localhost:30091
```

Prometheus scrapes:

* `gateway-service`;
* `accounts-service`;
* `cash-service`;
* `transfer-service`;
* `notifications-service`;
* локальный `front-ui`.

## Бизнес-метрики

### `bank.cash.withdraw.failures`

Tags:

```text
login
```

Метрика инкрементируется в `CashService.java` при фактической неуспешной попытке снятия денег.

### `bank.transfer.failures`

Tags:

```text
senderLogin
recipientLogin
```

Метрика инкрементируется в `TransferService.java` при фактической неуспешной попытке перевода денег.

### `bank.notifications.failures`

Tags:

```text
login
```

Метрика инкрементируется в `NotificationService.java` при невозможности отправки или обработки уведомления.

## Что смотреть в Prometheus

HTTP RPS:

```promql
sum(rate(http_server_requests_seconds_count[5m])) by (application)
```

HTTP 4xx:

```promql
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) by (application)
```

HTTP 5xx:

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
```

p95 latency:

```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))
```

Failed withdrawals:

```promql
sum(bank_cash_withdraw_failures_total) by (login)
```

Failed transfers:

```promql
sum(bank_transfer_failures_total) by (senderLogin, recipientLogin)
```

Failed notifications:

```promql
sum(bank_notifications_failures_total) by (login)
```

## Алерты

Алерты реализованы в Prometheus и находятся в сабчарте Prometheus зонтичного Helm-чарта.

Пороговые значения:

| Alert                         | Условие                                                |
| ----------------------------- | ------------------------------------------------------ |
| `HighHttp5xxRate`             | доля HTTP 5xx выше `0.05` в течение `5m`               |
| `FailedWithdrawalsGrowing`    | `increase(bank_cash_withdraw_failures_total[5m]) >= 1` |
| `FailedTransfersGrowing`      | `increase(bank_transfer_failures_total[5m]) >= 1`      |
| `NotificationFailuresGrowing` | `increase(bank_notifications_failures_total[5m]) >= 1` |
| `PrometheusTargetDown`        | `up == 0` в течение `2m`                               |
| `HighHttpLatencyP95`          | p95 latency выше `1.5s` в течение `5m`                 |

## Grafana

Открыть Grafana:

```text
http://localhost:30093
```

Логин по умолчанию:

```text
login: admin
password: admin
```

Datasource на Prometheus и dashboard provisioning создаются автоматически через ConfigMap.

Предустановленный dashboard:

```text
My Bank Observability
```

На dashboard доступны:

* HTTP RPS;
* HTTP 4xx;
* HTTP 5xx;
* p95 latency;
* p99 latency;
* JVM heap memory;
* CPU usage;
* failed withdrawals;
* failed transfers;
* failed notifications.

## ELK и логирование

Все сервисы и `front-ui` используют единый JSON-формат логов через `logback-spring.xml`.

В логах есть поля:

* `timestamp`;
* `level`;
* `service`;
* `logger`;
* `thread`;
* `message`;
* `exception`;
* `traceId`;
* `spanId`.

Основные бизнес-операции логируются на уровне `info`.

Ошибки и сбои интеграций логируются на уровнях `warn` и `error`.

Открыть Kibana:

```text
http://localhost:30092
```

В Kubernetes развёрнуты:

* Elasticsearch;
* Logstash;
* Kibana.

Логи backend-сервисов из Kubernetes отправляются в `logstash:5000`.

Для локального `front-ui` используется `localhost:30095`.

## Проверка логов

Открыть Kibana.

При первом запуске создать Data View:

```text
name: my-bank-logs-*
time field: @timestamp
```

Перейти в Discover.

Выполнить действие в UI.

Искать логи по полям:

* `service`;
* `traceId`;
* `spanId`;
* `login`.

Примеры поиска:

По сервису:

```text
service:"cash-service"
```

По trace id:

```text
traceId:"<trace-id-from-zipkin-or-log>"
```

Проверить логи Logstash:

```powershell
kubectl logs -n my-bank deploy/my-bank-logstash
```

## Полезные команды

Обновить релиз:

```powershell
helm upgrade my-bank .\helm\my-bank --namespace my-bank
```

Удалить релиз:

```powershell
helm uninstall my-bank --namespace my-bank
```
