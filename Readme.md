## Работа с замечаниями и предложениями Спринт 11:

1. Тест CashControllerKafkaIT исправлено:

@SpringBootTest(classes = CashServiceApplication.class, ...) - поднимается нормальный Boot-контекст, со всеми автоконфигами, включая KafkaTemplate.

Не ограничиваемся classes = CashController.class:

    KafkaTemplate<String, Notification> создаётся автоконфигом spring-kafka,
    CashController получает реальный KafkaTemplate и мокнутый WebClient.

Через @TestPropertySource:

    задаём топик app.kafka.topics.notifications,

    подключаем spring.kafka.bootstrap-servers к EmbeddedKafka,

    отключаем реактивную security-автоконфигурацию.


Вызываем именно публичный метод deposit(...), (“не тестировать приватные методы”):

    вход: параметры accountId и amount,

    результат: запись в Kafka-топике notifications.raw.

2. Тест TransferControllerKafkaIT, исправлено:

Исключена рефлексия — не трогаем приватный метод sendNotification(...).

Тест привязан к публичному API контроллера transfer(@RequestBody TransferRequest request).

Kafka-интеграция проверяется end-to-end через EmbeddedKafka.

TransferService замокан — тест не зависит от бизнес-логики перевода и внешних HTTP-вызовов.

Security в тестовом контексте отключена через spring.autoconfigure.exclude и не мешает поднятию контекста.

3. Замечание: "сохраняешь сущность в БД (который repository.save), а в doOnSuccess отправляешь сообщение в Kafka. Если сообщение в Kafka не уйдет (сбой сети, брокер недоступен), у тебя в базе будет созданный аккаунт, но уведомление не отправится, и другие сервисы об этом не узнают".

Исправление (на примере account-service):

Для гарантии доставки события в сервисе account-service при создании нового счета пользователя применен паттерн Transactional Outbox.

После исправления, вместо того чтобы сразу слать сообщение в Kafka:

в одной БД-транзакции: сохраняем новый счет в Account и сохраняем Outbox-событие (которое нужно отправить в Kafka).

Отдельный компонент читает таблицу outbox и отправляет события в Kafka, помечая их как обработанные.

Т.е.: БД-коммит = и аккаунт, и запись в outbox.

Если транзакция не закоммитилась — нет ни аккаунта, ни события.

Изменения:

* добавлена Outbox-сущность;

* В БД bank создана схема/таблица outbox.outbox_events;

* добавлен OutboxProcessor;

* доработан AccountController.



## Скрины работы приложения:

Zipkin Dependencies

![Grafana allerting](img/zipkin_dependencis.jpg)

Zipkin Transfer

![Grafana allerting](img/zipkin_transfer.jpg)

Prometheus

![Grafana allerting](img/prometheus.jpg)

Grafana DashBoard 1

![Grafana DashBoard](img/ghrafana_busines.jpg)

Grafana DashBoard 2

![Grafana DashBoard](img/http_metrics.jpg)

Grafana DashBoard 3

![Grafana DashBoard](img/JVM_metrics.jpg)

Grafana allerting

![Grafana allerting](img/allerting.jpg)






