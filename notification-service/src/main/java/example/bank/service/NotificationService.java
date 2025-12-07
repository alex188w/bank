package example.bank.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import example.bank.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

@Slf4j
@Service
public class NotificationService {

    private final Sinks.Many<Notification> sink = Sinks.many().replay().limit(1);

    public void publish(Notification notification) {
        if (notification == null) {
            return;
        }
        try {
            log.info("Уведомление: {}", notification);
            sink.tryEmitNext(notification);
        } catch (Exception e) {
            log.error("Ошибка при обработке уведомления в NotificationService", e);
        }
    }

    public Flux<Notification> streamNotifications() {
        return sink.asFlux()
                .mergeWith(Flux.interval(Duration.ofSeconds(15))
                        .map(tick -> new Notification(
                                "keep-alive",
                                null,
                                null,
                                null,
                                "keep-alive " + LocalTime.now(),
                                null,
                                null)))
                .filter(Objects::nonNull)
                .share();
    }
}
