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

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import example.bank.service.TransferService;

@Slf4j
@RestController
@RequestMapping(path = "/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final KafkaTemplate<String, Notification> kafkaTemplate;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    @PostMapping
    public Mono<Void> transfer(@RequestBody TransferRequest request) {
        log.info("Transfer request: {}({}) → {}({}), сумма={}",
                request.getFromUsername(), request.getFromId(),
                request.getToUsername(), request.getToId(),
                request.getAmount());

        return transferService.transfer(
                        request.getFromUsername(),
                        request.getFromId(),
                        request.getToUsername(),
                        request.getToId(),
                        request.getAmount()
                )
                .then(sendNotification(request))
                .doOnSuccess(v -> log.info("Transfer OK + notification sent (async via Kafka)"))
                .then();
    }

    private Mono<Void> sendNotification(TransferRequest request) {
        BigDecimal amount = request.getAmount();

        String message = String.format(
                "Перевод %.2f со счёта №%d → на счёт №%d (владелец: %s)",
                amount,
                request.getFromId(),
                request.getToId(),
                request.getToUsername()
        );

        Notification notification = new Notification(
                "transfer",
                request.getToUsername(),
                request.getFromId(),
                request.getToId(),
                message,
                amount,
                request.getToId()
        );

        return Mono.fromFuture(
                        kafkaTemplate.send(
                                notificationsTopic,
                                String.valueOf(request.getToId()),
                                notification
                        )
                )
                .doOnSuccess(this::logSuccess)
                .doOnError(e -> log.error("Ошибка при отправке уведомления о переводе в Kafka", e))
                .then();
    }

    private void logSuccess(SendResult<String, Notification> result) {
        RecordMetadata meta = result.getRecordMetadata();
        log.info("📨 Notification sent: topic={}, partition={}, offset={}",
                meta.topic(), meta.partition(), meta.offset());
    }
}
