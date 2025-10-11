package example.bank.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/exchange")
public class ExchangeController {

    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam String from,
                                       @RequestParam String to,
                                       @RequestParam BigDecimal amount) {
        BigDecimal rate = getRate(from, to);
        BigDecimal result = amount.multiply(rate);
        return Map.of(
            "from", from,
            "to", to,
            "amount", amount,
            "rate", rate,
            "result", result
        );
    }

    private BigDecimal getRate(String from, String to) {
        if (from.equals(to)) return BigDecimal.ONE;
        // временно зададим фиктивные курсы
        return switch (from + "_" + to) {
            case "USD_RUB" -> BigDecimal.valueOf(95.5);
            case "RUB_USD" -> BigDecimal.valueOf(0.0105);
            case "EUR_RUB" -> BigDecimal.valueOf(100.8);
            case "RUB_EUR" -> BigDecimal.valueOf(0.0099);
            case "USD_EUR" -> BigDecimal.valueOf(0.93);
            case "EUR_USD" -> BigDecimal.valueOf(1.07);
            default -> BigDecimal.ONE;
        };
    }
}