package example.bank.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


import reactor.core.publisher.Flux;
import example.bank.service.NotificationService;
import example.bank.dto.Notification;
import lombok.RequiredArgsConstructor;


@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor

public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Notification> stream() {
        log.info("Клиент подключился к потоку уведомлений");
        return notificationService.streamNotifications()
                .doOnNext(n -> log.info("Отправка уведомления в SSE: {}", n));
    }

    @PostMapping
    public void notifyUser(@RequestBody Notification notification) {
        notificationService.sendNotification(notification);
    }
}
