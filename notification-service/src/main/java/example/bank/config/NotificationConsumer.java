package example.bank.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import example.bank.Notification;
import example.bank.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "notifications.raw",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Notification notification) {

        log.info("📩 Kafka notification received: {}", notification);

        // КЛЮЧЕВОЕ МЕСТО
        notificationService.publish(notification);
    }
}
