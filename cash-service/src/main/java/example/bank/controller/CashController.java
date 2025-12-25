package example.bank.controller;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import example.bank.Notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@RestController
@RequestMapping(path = "/cash", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CashController {

        private final WebClient accountWebClient;

        private final @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, Notification> kafkaTemplate;

        private final MeterRegistry meterRegistry;
        private final CashObservability audit;

        @Value("${app.kafka.topics.notifications}")
        private String notificationsTopic;

        private Mono<String> currentLogin() {
                return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication())
                                .map(auth -> auth != null ? auth.getName() : "unknown")
                                .defaultIfEmpty("unknown");
        }

        private void inc(String metric, String result, String login, String accountId) {
                Counter.builder(metric)
                                .tag("result", result)
                                .tag("login", login)
                                .tag("account_id", accountId)
                                .register(meterRegistry)
                                .increment();
        }

        private void incNotifFail(String login, String accountId, String type) {
                Counter.builder("cash_notification_send_failed_total")
                                .tag("login", login)
                                .tag("account_id", accountId)
                                .tag("type", type)
                                .register(meterRegistry)
                                .increment();
        }

        @PostMapping("/deposit/{accountId}")
        public Mono<Void> deposit(ServerWebExchange exchange,
                        @PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {

                log.info("Deposit request: id={}, amount={}", accountId, amount);

                return currentLogin().flatMap(login -> {
                        audit.depositRequested(exchange, login, accountId);

                        // безопасно прокинуть traceparent дальше 
                        String tp = exchange.getRequest().getHeaders().getFirst("traceparent");

                        return accountWebClient.post()
                                        .uri("/accounts/{id}/deposit?amount={amount}", accountId, amount)
                                        .headers(h -> {
                                                if (tp != null)
                                                        h.set("traceparent", tp);
                                        })
                                        .retrieve()
                                        .bodyToMono(Void.class)
                                        .then(sendNotification(exchange, accountId, "deposit", amount, null, login))
                                        .doOnSuccess(v -> {
                                                inc("cash_deposit_total", "success", login, String.valueOf(accountId));
                                                audit.depositSuccess(exchange, login, accountId);
                                        })
                                        .doOnError(e -> {
                                                inc("cash_deposit_total", "failure", login, String.valueOf(accountId));
                                                audit.depositFailed(exchange, login, accountId, e);
                                        });
                });
        }

        @PostMapping("/withdraw/{accountId}")
        public Mono<Void> withdraw(ServerWebExchange exchange,
                        @PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {

                log.info("Withdraw request: id={}, amount={}", accountId, amount);

                return currentLogin().flatMap(login -> {
                        audit.withdrawRequested(exchange, login, accountId);

                        String tp = exchange.getRequest().getHeaders().getFirst("traceparent");

                        return accountWebClient.post()
                                        .uri("/accounts/{id}/withdraw?amount={amount}", accountId, amount)
                                        .headers(h -> {
                                                if (tp != null)
                                                        h.set("traceparent", tp);
                                        })
                                        .retrieve()
                                        .bodyToMono(Void.class)
                                        .then(sendNotification(exchange, accountId, "withdraw", amount, null, login))
                                        .doOnSuccess(v -> {
                                                inc("cash_withdraw_total", "success", login, String.valueOf(accountId));
                                                audit.withdrawSuccess(exchange, login, accountId);
                                        })
                                        .doOnError(e -> {
                                                inc("cash_withdraw_total", "failure", login, String.valueOf(accountId));
                                                audit.withdrawFailed(exchange, login, accountId, e);
                                        });
                });
        }

        private Mono<Void> sendNotification(ServerWebExchange exchange,
                        Long accountId,
                        String type,
                        BigDecimal amount,
                        String customMessage,
                        String login) {

                // amount в лог не пишем, но в уведомлении он есть (это не лог)
                String message = switch (type.toLowerCase()) {
                        case "deposit" -> String.format("Пополнение счёта №%d на сумму %.2f", accountId, amount);
                        case "withdraw" -> String.format("Снятие со счёта №%d на сумму %.2f", accountId, amount);
                        default -> (customMessage != null)
                                        ? customMessage
                                        : String.format("Операция со счётом №%d: сумма %.2f", accountId, amount);
                };

                Notification notification = new Notification(
                                type,
                                null,
                                null,
                                null,
                                message,
                                amount,
                                accountId);

                return Mono.fromFuture(kafkaTemplate.send(notificationsTopic, String.valueOf(accountId), notification))
                                .doOnSuccess(result -> {
                                        logKafkaSuccess(result);
                                        audit.notificationSent(exchange, login, accountId, type);
                                })
                                .doOnError(e -> {
                                        log.error("Ошибка при отправке уведомления в Kafka: {}", e.getMessage(), e);
                                        incNotifFail(login, String.valueOf(accountId), type);
                                        audit.notificationFailed(exchange, login, accountId, type, e);
                                })
                                .then();
        }

        private void logKafkaSuccess(SendResult<String, Notification> result) {
                var m = result.getRecordMetadata();
                log.info("Отправлено уведомление в Kafka: topic={}, partition={}, offset={}",
                                m.topic(), m.partition(), m.offset());
        }
}
