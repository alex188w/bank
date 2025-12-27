package example.bank.controller;

import example.bank.Notification;
import example.bank.logging.CashAudit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping(path = "/cash", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CashController {

        private final WebClient accountWebClient;

        private final @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, Notification> kafkaTemplate;

        private final MeterRegistry meterRegistry;
        private final CashAudit audit;

        @Value("${app.kafka.topics.notifications}")
        private String notificationsTopic;

        private Mono<String> currentLogin() {
                return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication())
                                .map(auth -> auth != null ? auth.getName() : "unknown")
                                .defaultIfEmpty("unknown");
        }

        private void incBusiness(String type, String status) {
                Counter.builder("business_operation_total")
                                .tag("service", "cash")
                                .tag("type", type) // deposit/withdraw/notify
                                .tag("status", status) // success/failure
                                .register(meterRegistry)
                                .increment();
        }

        private void incNotifFail(String type) {
                Counter.builder("cash_notification_send_failed_total")
                                .tag("type", type)
                                .register(meterRegistry)
                                .increment();
        }

        @PostMapping("/deposit/{accountId}")
        @Transactional
        public Mono<Void> deposit(@RequestHeader(value = "traceparent", required = false) String traceparent,
                        @PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {

                return currentLogin().flatMap(login -> {
                        audit.info("cash.deposit", "deposit", "requested",
                                        "Deposit requested login=" + login + " accountId=" + accountId
                                                        + " amount=\"\"");

                        return callAccount(traceparent, "/accounts/{id}/deposit?amount={amount}", accountId, amount)
                                        .then(sendNotification(accountId, "deposit", amount, login))
                                        .doOnSuccess(v -> {
                                                incBusiness("deposit", "success");
                                                audit.info("cash.deposit", "deposit", "success",
                                                                "Deposit success login=" + login + " accountId="
                                                                                + accountId + " amount=\"\"");
                                        })
                                        .doOnError(ex -> {
                                                incBusiness("deposit", "failure");
                                                audit.error("cash.deposit", "deposit", "failure",
                                                                "Deposit failed login=" + login + " accountId="
                                                                                + accountId + " amount=\"\"",
                                                                ex);
                                        });
                });
        }

        @PostMapping("/withdraw/{accountId}")
        @Transactional
        public Mono<Void> withdraw(@RequestHeader(value = "traceparent", required = false) String traceparent,
                        @PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {

                return currentLogin().flatMap(login -> {
                        audit.info("cash.withdraw", "withdraw", "requested",
                                        "Withdraw requested login=" + login + " accountId=" + accountId
                                                        + " amount=\"\"");

                        return callAccount(traceparent, "/accounts/{id}/withdraw?amount={amount}", accountId, amount)
                                        .then(sendNotification(accountId, "withdraw", amount, login))
                                        .doOnSuccess(v -> {
                                                incBusiness("withdraw", "success");
                                                audit.info("cash.withdraw", "withdraw", "success",
                                                                "Withdraw success login=" + login + " accountId="
                                                                                + accountId + " amount=\"\"");
                                        })
                                        .doOnError(ex -> {
                                                incBusiness("withdraw", "failure");
                                                audit.error("cash.withdraw", "withdraw", "failure",
                                                                "Withdraw failed login=" + login + " accountId="
                                                                                + accountId + " amount=\"\"",
                                                                ex);
                                        });
                });
        }

        private Mono<Void> callAccount(String traceparent, String uri, Long accountId, BigDecimal amount) {
                return accountWebClient.post()
                                .uri(uri, accountId, amount)
                                .headers(h -> {
                                        if (traceparent != null && !traceparent.isBlank()) {
                                                h.set("traceparent", traceparent);
                                        }
                                })
                                .retrieve()
                                .bodyToMono(Void.class);
        }

        private Mono<Void> sendNotification(Long accountId, String type, BigDecimal amount, String login) {
                // amount в лог НЕ пишем, но в уведомлении он есть (это не лог)
                String message = switch (type) {
                        case "deposit" -> String.format("Пополнение счёта №%d на сумму %.2f", accountId, amount);
                        case "withdraw" -> String.format("Снятие со счёта №%d на сумму %.2f", accountId, amount);
                        default -> String.format("Операция со счётом №%d: сумма %.2f", accountId, amount);
                };

                Notification n = new Notification(
                                type,
                                null,
                                null,
                                null,
                                message,
                                amount,
                                accountId);

                return Mono.fromFuture(kafkaTemplate.send(notificationsTopic, String.valueOf(accountId), n))
                                .doOnSuccess(this::logKafkaSuccess)
                                .doOnSuccess(r -> {
                                        incBusiness("notify", "success");
                                        audit.info("cash.notify", "notify", "success",
                                                        "Notification sent login=" + login + " accountId=" + accountId
                                                                        + " type=" + type + " amount=\"\"");
                                })
                                .doOnError(ex -> {
                                        incBusiness("notify", "failure");
                                        incNotifFail(type);
                                        audit.error("cash.notify", "notify", "failure",
                                                        "Notification failed login=" + login + " accountId=" + accountId
                                                                        + " type=" + type + " amount=\"\"",
                                                        ex);
                                })
                                .then();
        }

        private void logKafkaSuccess(SendResult<String, Notification> result) {
                var m = result.getRecordMetadata();
                log.info("Kafka notification ok topic={} partition={} offset={}", m.topic(), m.partition(), m.offset());
        }
}
