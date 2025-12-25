package example.bank.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import example.bank.Notification;
import example.bank.model.OutboxEvent;
import example.bank.repository.OutboxEventRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class OutboxProcessor {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxEventRepository outboxRepository,
            @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, Notification> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${app.outbox.enabled:true}")
    private boolean enabled;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("OutboxProcessor disabled");
            return;
        }

        Flux.interval(Duration.ofSeconds(1))
                .concatMap(tick -> processBatch()) // <-- без параллельных тиков
                .onErrorContinue((e, o) -> log.error("Ошибка при обработке outbox", e))
                .subscribe();
    }

    private Mono<Void> processBatch() {
        return outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()
                .concatMap(this::processEvent) // <-- события тоже последовательно
                .then();
    }

    private Mono<Void> processEvent(OutboxEvent event) {
        final Notification notification;
        try {
            notification = objectMapper.readValue(event.getPayload(), Notification.class);
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации payload outbox id={}", event.getId(), e);
            return Mono.empty();
        }

        String key = event.getAggregateId();

        return Mono.fromFuture(kafkaTemplate.send(notificationsTopic, key, notification))
                .doOnSuccess(
                        result -> log.info("Outbox->Kafka отправлено: eventId={}, topic={}, partition={}, offset={}",
                                event.getId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()))
                .then(markProcessed(event))
                .onErrorResume(ex -> {
                    log.error("Outbox->Kafka send failed: eventId={}, topic={}", event.getId(), notificationsTopic, ex);
                    return Mono.empty();
                });
    }

    private Mono<Void> markProcessed(OutboxEvent event) {
        event.setProcessed(true);
        return outboxRepository.save(event).then();
    }
}