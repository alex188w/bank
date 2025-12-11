package example.bank.controller;

import org.springframework.stereotype.Component;
import example.bank.Notification;
import example.bank.model.OutboxEvent;
import jakarta.annotation.PostConstruct;
import example.bank.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Notification> kafkaTemplate;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void start() {
        // Простой пуллинг каждые N миллисекунд
        Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> processBatch())
                .onErrorContinue((e, o) -> log.error("Ошибка при обработке outbox", e))
                .subscribe();
    }

    private Mono<Void> processBatch() {
        return outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()
                .flatMap(this::processEvent)
                .then();
    }

    private Mono<Void> processEvent(OutboxEvent event) {
        Notification notification;
        try {
            notification = objectMapper.readValue(event.getPayload(), Notification.class);
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации payload outbox id={}", event.getId(), e);
            // можно пометить как processed с ошибкой
            return Mono.empty();
        }

        String key = event.getAggregateId();

        return Mono.fromFuture(
                        kafkaTemplate.send(
                                notificationsTopic,
                                key,
                                notification)
                )
                .doOnSuccess(result -> log.info("Outbox->Kafka отправлено: eventId={}, topic={}, partition={}, offset={}",
                        event.getId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()))
                .then(markProcessed(event));
    }

    private Mono<Void> markProcessed(OutboxEvent event) {
        event.setProcessed(true);
        return outboxRepository.save(event).then();
    }
}