package example.bank.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

        private final WebClient accountWebClient;
        private final WebClient notificationWebClient;

        @PostMapping("/deposit/{accountId}")
        public Mono<Void> deposit(@PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {
                return accountWebClient.post()
                                .uri("/accounts/{id}/deposit?amount={amount}", accountId, amount)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .then(sendNotification(accountId, "deposit", amount, null));
        }

        @PostMapping("/withdraw/{accountId}")
        public Mono<Void> withdraw(@PathVariable Long accountId,
                        @RequestParam BigDecimal amount) {
                return accountWebClient.post()
                                .uri("/accounts/{id}/withdraw?amount={amount}", accountId, amount)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .then(sendNotification(accountId, "withdraw", amount, null));
        }

        private Mono<Void> sendNotification(Long accountId, String type, BigDecimal amount, String customMessage) {
                String message = switch (type.toLowerCase()) {
                        case "deposit" -> String.format("Пополнение счёта №%d на сумму %.2f", accountId, amount);
                        case "withdraw" -> String.format("Снятие со счёта №%d на сумму %.2f", accountId, amount);
                        default -> customMessage != null
                                        ? customMessage
                                        : String.format("Операция со счётом №%d: сумма %.2f", accountId, amount);
                };

                Map<String, Object> notification = Map.of(
                                "accountId", accountId,
                                "type", type,
                                "amount", amount,
                                "message", message);

                return notificationWebClient.post()
                                .uri("/notifications")
                                .bodyValue(notification)
                                .retrieve()
                                .toBodilessEntity()
                                .then()
                                .doOnSuccess(v -> log.info("Отправлено уведомление: {}", notification))
                                .doOnError(e -> log.error("Ошибка при отправке уведомления: {}", e.getMessage()));
        }
}
