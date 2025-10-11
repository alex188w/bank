package example.bank.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cash")
public class CashController {

    private final WebClient plainWebClient; // теперь этот бин будет обычным HTTP клиентом

    @PostMapping("/deposit/{accountId}")
    public Mono<Void> deposit(@PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        return plainWebClient.post()
                .uri("http://localhost:8082/accounts/{id}/deposit?amount={amount}", accountId, amount)
                .retrieve()
                .bodyToMono(String.class) // <- строка вместо Void
                .then(); // чтобы вернуть Mono<Void>
    }

    @PostMapping("/withdraw/{accountId}")
    public Mono<Void> withdraw(@PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        return plainWebClient.post()
                .uri("http://localhost:8082/accounts/{id}/withdraw?amount={amount}", accountId, amount)
                .retrieve()
                .bodyToMono(String.class) // <- строка вместо Void
                .then(); // чтобы вернуть Mono<Void>
    }
}

