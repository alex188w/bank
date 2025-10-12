package example.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealRateUpdater {

    @Value("${exchange.api-url}")
    private String apiUrl;

    @Value("${exchange.target-url}")
    private String targetUrl;

    private final WebClient webClient = WebClient.create();

    private static final List<String> CURRENCIES = List.of("RUB", "USD", "EUR");

    @PostConstruct
    public void init() {
        log.info("Первичная генерация курсов валют при старте приложения");
        updateRatesFromApi();
    }

    // 🔁 Раз в минуту
    @Scheduled(fixedRateString = "${exchange.update-interval-ms:60000}")
    public void updateRatesFromApi() {
        log.info("🔄 Запрашиваем реальные курсы валют...");

        webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(Map.class) // возвращаем Map
                .flatMap(response -> extractAndSendRates((Map<String, Object>) response))
                .onErrorResume((Throwable e) -> {
                    log.error("Ошибка при получении курсов: {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    private Mono<Void> extractAndSendRates(Map<String, Object> response) {
        try {
            Map<String, Object> ratesMap = (Map<String, Object>) response.get("rates");
            if (ratesMap == null) {
                log.warn("Ответ API не содержит поля 'rates'");
                return Mono.empty();
            }

            List<Map<String, Object>> converted = new ArrayList<>();

            for (String currency : CURRENCIES) {
                if (!ratesMap.containsKey(currency))
                    continue;
                double value = ((Number) ratesMap.get(currency)).doubleValue(); // важно: Number -> double
                double inverse = 1 / value;

                converted.add(Map.of(
                        "currency", currency,
                        "buy", BigDecimal.valueOf(inverse).setScale(2, RoundingMode.HALF_UP),
                        "sell", BigDecimal.valueOf(inverse * 1.01).setScale(2, RoundingMode.HALF_UP)));
            }

            log.info("📈 Отправляем курсы в exchange-service: {}", converted);

            return webClient.post()
                    .uri(targetUrl)
                    .bodyValue(converted)
                    .retrieve()
                    .toBodilessEntity()
                    .then();

        } catch (Exception e) {
            log.error("Ошибка при обработке курсов: {}", e.getMessage());
            return Mono.empty();
        }
    }
}