package example.bank.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import example.bank.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RestController
@RequestMapping(path = "/cash", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CashController {

        private final WebClient accountWebClient;
        private final KafkaTemplate<String, Notification> kafkaTemplate;

        @Value("${app.kafka.topics.notifications}")
        private String notificationsTopic;

        @PostMapping("/deposit/{accountId}")
        public Mono<Void> deposit(@PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {

                return accountWebClient.post()
                                .uri("/accounts/{id}/deposit?amount={amount}", accountId, amount)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .then(sendNotification(accountId, "deposit", amount, null));
        }

        @PostMapping("/withdraw/{accountId}")
        public Mono<Void> withdraw(@PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {

                return accountWebClient.post()
                                .uri("/accounts/{id}/withdraw?amount={amount}", accountId, amount)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .then(sendNotification(accountId, "withdraw", amount, null));
        }

        private Mono<Void> sendNotification(Long accountId,
                        String type,
                        BigDecimal amount,
                        String customMessage) {

                String message = switch (type.toLowerCase()) {
                        case "deposit" ->
                                String.format("Пополнение счёта №%d на сумму %.2f", accountId, amount);
                        case "withdraw" ->
                                String.format("Снятие со счёта №%d на сумму %.2f", accountId, amount);
                        default -> customMessage != null
                                        ? customMessage
                                        : String.format("Операция со счётом №%d: сумма %.2f", accountId, amount);
                };

                // Notification из общего модуля bank-events
                Notification notification = new Notification(
                                type, // type: deposit / withdraw / ...
                                null, // username (здесь можем не знать, оставляем null)
                                null, // fromId
                                null, // toId
                                message, // текст сообщения
                                amount, // сумма
                                accountId // accountId
                );

                return Mono.fromFuture(
                                kafkaTemplate.send(
                                                notificationsTopic,
                                                String.valueOf(accountId),
                                                notification))
                                .doOnSuccess(result -> log.info(
                                                "Отправлено уведомление в Kafka: topic={}, partition={}, offset={}, payload={}",
                                                result.getRecordMetadata().topic(),
                                                result.getRecordMetadata().partition(),
                                                result.getRecordMetadata().offset(),
                                                notification))
                                .doOnError(e -> log.error("Ошибка при отправке уведомления в Kafka: {}", e.getMessage(),
                                                e))
                                .then();
        }
}
