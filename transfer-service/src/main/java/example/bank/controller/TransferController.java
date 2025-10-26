package example.bank.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import example.bank.dto.TransferRequest;
import example.bank.service.TransferService;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final WebClient notificationWebClient;

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
                request.getAmount())
                .then(sendNotification(request));
    }

    private Mono<Void> sendNotification(TransferRequest request) {
        Map<String, Object> notification = Map.of(
                "type", "transfer",
                "username", request.getToUsername(),
                "fromId", request.getFromId(),
                "toId", request.getToId(),
                "message", String.format("Перевод %.2f со счёта: №%d → на счет: №%d (владелец: %s)",
                        request.getAmount(),
                        request.getFromId(),
                        request.getToId(),
                        request.getToUsername()),
                "amount", request.getAmount(),
                "accountId", request.getToId());

        return notificationWebClient.post()
                .uri("/notifications")
                .bodyValue(notification)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnSuccess(v -> log.info("Отправлено уведомление о переводе: {}", notification))
                .doOnError(e -> log.error("Ошибка при отправке уведомления: {}", e.getMessage()));
    }
}
