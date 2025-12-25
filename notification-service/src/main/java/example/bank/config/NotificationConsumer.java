package example.bank.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import example.bank.Notification;
import example.bank.service.NotificationAudit;
import example.bank.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationAudit audit;

    @KafkaListener(topics = "notifications.raw", groupId = "notification-service", containerFactory = "kafkaListenerContainerFactory")
    public void consume(Notification notification) {

        // без секретов
        audit.info("notification.kafka.consume",
                "Kafka notification received: type=" + safe(notification.getType())
                        + " username=" + safe(notification.getUsername()));

        notificationService.publish(notification);
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
