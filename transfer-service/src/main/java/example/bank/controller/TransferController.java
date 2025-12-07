package example.bank.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import example.bank.dto.TransferRequest;
import example.bank.service.TransferService;
import reactor.core.publisher.Mono;
import example.bank.Notification; // из модуля bank-events
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.math.BigDecimal;

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
        log.info("Transfer request: {} → {} ({}), сумма={}",
                request.getFromId(), request.getToId(),
                request.getToUsername(), request.getAmount());

        return transferService.transfer(
                        request.getFromUsername(),
                        request.getFromId(),
                        request.getToUsername(),
                        request.getToId(),
                        request.getAmount()
                )
                .then(sendNotification(request));
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
                request.getToUsername(),         // username
                request.getFromId(),             // fromId
                request.getToId(),               // toId
                message,                         // message
                amount,                          // amount
                request.getToId()                // accountId (получатель)
        );

        Mono<SendResult<String, Notification>> sendMono =
                Mono.fromFuture(
                        kafkaTemplate.send(
                                notificationsTopic,
                                String.valueOf(request.getToId()),
                                notification
                        )
                );

        return sendMono
                .doOnSuccess(this::logSuccess)
                .doOnError(e -> log.error("Ошибка при отправке уведомления о переводе в Kafka", e))
                .then();
    }

    private void logSuccess(SendResult<String, Notification> result) {
        RecordMetadata meta = result.getRecordMetadata();
        log.info("Отправлено уведомление о переводе в Kafka: topic={}, partition={}, offset={}",
                meta.topic(), meta.partition(), meta.offset());
    }
}
