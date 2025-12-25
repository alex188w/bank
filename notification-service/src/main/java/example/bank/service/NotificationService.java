package example.bank.service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Random;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.Gauge;

import example.bank.Notification;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

    private final NotificationAudit audit;

    private final MeterRegistry meterRegistry;

    private final Sinks.Many<Notification> sink = Sinks.many().replay().limit(1);
    private final AtomicInteger activeSubscribers = new AtomicInteger(0);
    private final Random random = new Random();

    @Value("${app.notifications.simulate-failure.enabled:false}")
    private boolean simulateFailureEnabled;

    @Value("${app.notifications.simulate-failure.probability:0.0}")
    private double simulateFailureProbability;

    @PostConstruct
    void initMeters() {
        Gauge.builder("notification_sse_active_subscribers", activeSubscribers, AtomicInteger::get)
                .register(meterRegistry);
    }

    private void incSseSend(String result, Notification n) {
        String login = (n != null) ? n.getUsername() : null;

        Counter.builder("notification_sse_send_total")
                .description("Count of notifications delivered to SSE subscribers")
                .tag("result", result) // success|failure
                .tag("login", safe(login))
                .register(meterRegistry)
                .increment();
    }

    public void publish(Notification notification) {
        if (notification == null)
            return;

        try {
            audit.info("notification.sse.publish",
                    "Publish notification to SSE: type=" + safe(notification.getType())
                            + " username=" + safe(notification.getUsername()));

            sink.tryEmitNext(notification);
        } catch (Exception e) {
            audit.error("notification.sse.publish", "Failed to publish to sink", e);
            incSseSend("failure", notification);
        }
    }

    public Flux<Notification> streamNotifications() {
        Flux<Notification> data = sink.asFlux()
                .mergeWith(
                        Flux.interval(Duration.ofSeconds(15))
                                .map(tick -> new Notification(
                                        "keep-alive",
                                        null, null, null,
                                        "keep-alive " + LocalTime.now(),
                                        null, null)))
                .filter(Objects::nonNull)
                .share();

        return data
                .doOnSubscribe(sub -> {
                    audit.info("notification.sse.subscribe", "SSE subscriber connected");
                    activeSubscribers.incrementAndGet();
                })
                .doFinally(sig -> {
                    audit.info("notification.sse.unsubscribe", "SSE subscriber disconnected");
                    activeSubscribers.decrementAndGet();
                })
                .flatMap(n -> {
                    // keep-alive не считаем
                    if ("keep-alive".equalsIgnoreCase(safe(n.getType()))) {
                        return Mono.just(n);
                    }

                    // Симуляция "недоставки" для статистики != 0
                    if (simulateFailureEnabled && simulateFailureProbability > 0
                            && random.nextDouble() < simulateFailureProbability) {
                        incSseSend("failure", n);
                        // имитируем "обрыв" для конкретного события — пропускаем его
                        return Mono.empty();
                    }

                    incSseSend("success", n);
                    return Mono.just(n);
                })
                // Важно: не даём всей SSE-стриме умереть от ошибки
                .onErrorResume(e -> {
                    log.error("SSE stream error", e);
                    // считаем как failure без привязки к уведомлению
                    incSseSend("failure", null);
                    return Flux.empty();
                });
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
