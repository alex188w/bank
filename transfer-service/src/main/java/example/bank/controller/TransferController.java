package example.bank.controller;

import java.math.BigDecimal;

import example.bank.Notification;
import example.bank.dto.TransferRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import example.bank.service.TransferService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@Slf4j
@RestController
@RequestMapping(path = "/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransferController {

        private final TransferService transferService;
        private final KafkaTemplate<String, Notification> kafkaTemplate;

        private final MeterRegistry meterRegistry;
        private final SuspiciousTransferBlocker blocker;

        @Value("${app.kafka.topics.notifications}")
        private String notificationsTopic;

        // === Metrics ===

        private Mono<String> currentLogin() {
                return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication())
                                .map(auth -> auth != null ? auth.getName() : "unknown")
                                .defaultIfEmpty("unknown");
        }

        private void incTransferTotal(String result, String fromLogin, TransferRequest r) {
                Counter.builder("transfer_total")
                                .tag("result", result)
                                .tag("from_login", safe(fromLogin))
                                .tag("to_login", safe(r.getToUsername()))
                                .tag("from_account", String.valueOf(r.getFromId()))
                                .tag("to_account", String.valueOf(r.getToId()))
                                .register(meterRegistry)
                                .increment();
        }

        private void incTransferTotalByUser(String result, String fromLogin) {
                Counter.builder("transfer_by_user")
                                .tag("result", result)
                                .tag("from_login", safe(fromLogin))
                                .register(meterRegistry)
                                .increment();
        }

        private void recordTransferAmountByUser(String fromLogin, BigDecimal amount) {
                if (amount == null)
                        return;

                DistributionSummary.builder("transfer_amount_by_user")
                                .tag("from_login", safe(fromLogin))
                                .register(meterRegistry)
                                .record(amount.doubleValue());
        }

        private void incNotifTotal(String result, String fromLogin, TransferRequest r) {
                Counter.builder("transfer_notification_send_total")
                                .tag("result", result)
                                .tag("from_login", safe(fromLogin))
                                .tag("to_login", safe(r.getToUsername()))
                                .tag("to_account", String.valueOf(r.getToId()))
                                .register(meterRegistry)
                                .increment();
        }

        private void incFailed(String fromLogin, TransferRequest r, String reason) {
                Counter.builder("transfer_failed_total")
                                .tag("reason", reason)
                                .tag("from_login", safe(fromLogin))
                                .tag("to_login", safe(r.getToUsername()))
                                .tag("from_account", String.valueOf(r.getFromId()))
                                .tag("to_account", String.valueOf(r.getToId()))
                                .register(meterRegistry)
                                .increment();
        }

        private void incBlocked(String fromLogin, TransferRequest r) {
                Counter.builder("transfer_blocked_total")
                                .tag("from_login", safe(fromLogin))
                                .tag("to_login", safe(r.getToUsername()))
                                .tag("from_account", String.valueOf(r.getFromId()))
                                .tag("to_account", String.valueOf(r.getToId()))
                                .register(meterRegistry)
                                .increment();
        }

        private void incNotificationFail(String fromLogin, TransferRequest r) {
                Counter.builder("transfer_notification_send_failed_total")
                                .tag("from_login", safe(fromLogin))
                                .tag("to_login", safe(r.getToUsername()))
                                .tag("to_account", String.valueOf(r.getToId()))
                                .register(meterRegistry)
                                .increment();
        }

        private String safe(String v) {
                return (v == null || v.isBlank()) ? "unknown" : v;
        }

        @PostMapping
        public Mono<Void> transfer(@RequestBody TransferRequest request) {
                return currentLogin().flatMap(fromLogin -> {

                        log.info("Transfer request: from_login={} {}({}) → {}({}), amount={}",
                                        fromLogin,
                                        request.getFromUsername(), request.getFromId(),
                                        request.getToUsername(), request.getToId(),
                                        request.getAmount());

                        // Blocker
                        if (blocker.shouldBlock()) {
                                incBlocked(fromLogin, request);
                                incFailed(fromLogin, request, "blocked");
                                incTransferTotal("failure", fromLogin, request);
                                return Mono.error(new IllegalStateException("Transfer blocked as suspicious"));
                        }

                        Mono<Void> business = transferService.transfer(
                                        request.getFromUsername(),
                                        request.getFromId(),
                                        request.getToUsername(),
                                        request.getToId(),
                                        request.getAmount())
                                        .doOnError(e -> incFailed(fromLogin, request, "business"));

                        Mono<Void> notify = sendNotification(request, fromLogin)
                                        .doOnError(e -> {
                                                incNotificationFail(fromLogin, request);
                                                incFailed(fromLogin, request, "notification");
                                        });

                        return business
                                        .then(notify)
                                        .doOnSuccess(v -> {
                                                incTransferTotal("success", fromLogin, request);
                                                incTransferTotalByUser("success", fromLogin);
                                                recordTransferAmountByUser(fromLogin, request.getAmount());
                                                log.info("Transfer OK + notification sent (async via Kafka)");
                                        })
                                        .doOnError(e -> {
                                                incTransferTotal("failure", fromLogin, request);
                                                incTransferTotalByUser("failure", fromLogin); // ✅ NEW
                                        })
                                        .then();
                });
        }

        private Mono<Void> sendNotification(TransferRequest request, String fromLogin) {
                BigDecimal amount = request.getAmount();

                String message = String.format(
                                "Перевод %.2f со счёта №%d → на счёт №%d (владелец: %s)",
                                amount,
                                request.getFromId(),
                                request.getToId(),
                                request.getToUsername());

                Notification notification = new Notification(
                                "transfer",
                                request.getToUsername(),
                                request.getFromId(),
                                request.getToId(),
                                message,
                                amount,
                                request.getToId());

                return Mono.fromFuture(
                                kafkaTemplate.send(
                                                notificationsTopic,
                                                String.valueOf(request.getToId()),
                                                notification))
                                .doOnSuccess(res -> {
                                        logSuccess(res);
                                        incNotifTotal("success", fromLogin, request);
                                })
                                .doOnError(e -> {
                                        log.error("Ошибка при отправке уведомления о переводе в Kafka", e);
                                        incNotifTotal("failure", fromLogin, request);
                                })
                                .then();
        }

        private void logSuccess(SendResult<String, Notification> result) {
                RecordMetadata meta = result.getRecordMetadata();
                log.info("📨 Notification sent: topic={}, partition={}, offset={}",
                                meta.topic(), meta.partition(), meta.offset());
        }
}
