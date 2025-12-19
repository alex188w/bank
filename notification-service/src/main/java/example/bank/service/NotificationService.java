package example.bank.service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;

import example.bank.Notification;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

    private final MeterRegistry meterRegistry;

    private final Sinks.Many<Notification> sink = Sinks.many().replay().limit(1);

    // Для gauge активных подписок (опционально)
    private final AtomicInteger activeSubscribers = new AtomicInteger(0);

    private void incSseSend(String result, Notification n) {
        String login = n != null ? n.getUsername() : null;
        Counter.builder("notification_sse_send_total")
                .tag("result", result)
                .tag("login", safe(login))
                .register(meterRegistry)
                .increment();
    }

    public void publish(Notification notification) {
        if (notification == null)
            return;
        try {
            log.info("Publish notification to SSE: {}", notification);
            sink.tryEmitNext(notification);
        } catch (Exception e) {
            log.error("Ошибка при обработке уведомления", e);
        }
    }

    public Flux<Notification> streamNotifications() {
        return sink.asFlux()
                .mergeWith(
                        Flux.interval(Duration.ofSeconds(15))
                                .map(tick -> new Notification(
                                        "keep-alive",
                                        null, null, null,
                                        "keep-alive " + LocalTime.now(),
                                        null, null)))
                .filter(Objects::nonNull)
                .doOnNext(n -> {
                    String type = n.getType();
                    if (type != null && "keep-alive".equalsIgnoreCase(type)) {
                        return;
                    }
                    incSseSend("success", n);
                })
                .doOnError(e -> Counter.builder("notification_sse_send_total")
                        .tag("result", "failure")
                        .tag("login", "unknown")
                        .register(meterRegistry)
                        .increment())
                .share();
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }

}
