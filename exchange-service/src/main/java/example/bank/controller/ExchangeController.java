package example.bank.controller;

import example.bank.service.ExchangeService;
import example.bank.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8080") // <- разрешаем фронт
public class ExchangeController {

    private final ExchangeService exchangeService;

    // Получить все текущие курсы
    @GetMapping("/rates")
    public Flux<ExchangeRate> getRates() {
        return Flux.fromIterable(exchangeService.getAllRates());
    }

    // Конвертация между валютами
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
    @PostMapping("/update")
    public Mono<Void> updateRates(@RequestBody List<ExchangeRate> newRates) {
        return Mono.fromRunnable(() -> exchangeService.updateRates(newRates));
    }
}