package example.bank.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final WebClient accountWebClient;

    @PostMapping("/deposit/{accountId}")
    public Mono<Void> deposit(@PathVariable Long accountId,
                              @RequestParam BigDecimal amount) {
        return accountWebClient.post()
                .uri("/accounts/{id}/deposit?amount={amount}", accountId, amount)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @PostMapping("/withdraw/{accountId}")
    public Mono<Void> withdraw(@PathVariable Long accountId,
                               @RequestParam BigDecimal amount) {
        return accountWebClient.post()
                .uri("/accounts/{id}/withdraw?amount={amount}", accountId, amount)
                .retrieve()
                .bodyToMono(Void.class);
    }
}

