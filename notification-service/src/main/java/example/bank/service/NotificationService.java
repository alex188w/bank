package example.bank.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import example.bank.dto.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalTime;

@Slf4j
@Service
public class NotificationService {

    private final Sinks.Many<Notification> sink = Sinks.many().replay().limit(1);
    // private final Sinks.Many<Notification> sink = Sinks.many().unicast().onBackpressureBuffer();
    // private final Sinks.Many<Notification> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void sendNotification(Notification notification) {
        log.info("Уведомление: {}", notification);
        sink.tryEmitNext(notification);
    }

    public Flux<Notification> streamNotifications() {
        return sink.asFlux()
                .mergeWith(Flux.interval(Duration.ofSeconds(15))
                        .map(tick -> new Notification(
                                "keep-alive", // строго так!
                                null,
                                null,
                                null,
                                "keep-alive " + LocalTime.now(),
                                null,
                                null)))
                .filter(n -> n != null) // защита
                .share();
    }
}
