package example.bank.controller;

import example.bank.ExchangeRate;
import example.bank.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8080")
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/rates")
    public Flux<ExchangeRate> getRates() {
        return Flux.fromIterable(exchangeService.getAllRates());
    }

    @GetMapping("/convert")
    public Mono<Map<String, Object>> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {

        return Mono.fromSupplier(() -> {
            BigDecimal result = exchangeService.convert(from, to, amount);
            return Map.of(
                    "from", from,
                    "to", to,
                    "amount", amount,
                    "result", result);
        });
    }

    // Приём обновлений от exchange-generator
    // @PostMapping("/update")
    // public Mono<Void> updateRates(@RequestBody List<ExchangeRate> newRates) {
    //     return Mono.fromRunnable(() -> exchangeService.updateRates(newRates));
    // }
}