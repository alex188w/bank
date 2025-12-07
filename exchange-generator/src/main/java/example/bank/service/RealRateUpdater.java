package example.bank.service;

import example.bank.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealRateUpdater {

    @Value("${exchange.api-url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.create();
    private final KafkaTemplate<String, ExchangeRate> kafkaTemplate;

    @Value("${app.kafka.topics.exchange-rates}")
    private String exchangeRatesTopic;

    private static final List<String> CURRENCIES = List.of("RUB", "USD", "EUR");

    @PostConstruct
    public void init() {
        log.info("Первичная генерация курсов валют при старте приложения");
        updateRatesFromApi();
    }

    @Scheduled(fixedRateString = "${exchange.update-interval-ms:60000}")
    public void updateRatesFromApi() {
        log.info("🔄 Запрашиваем реальные курсы валют...");

        webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> extractAndSendRates((Map<String, Object>) response))
                .onErrorResume(e -> {
                    log.error("Ошибка при получении курсов: {}", e.getMessage(), e);
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

            for (String currency : CURRENCIES) {
                if (!ratesMap.containsKey(currency)) {
                    continue;
                }
                double value = ((Number) ratesMap.get(currency)).doubleValue();
                double inverse = 1 / value;

                ExchangeRate rate = new ExchangeRate(
                        currency,
                        BigDecimal.valueOf(inverse).setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(inverse * 1.01).setScale(2, RoundingMode.HALF_UP));

                sendToKafka(rate).subscribe();
            }

            return Mono.empty();

        } catch (Exception e) {
            log.error("Ошибка при обработке курсов: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }

    private Mono<SendResult<String, ExchangeRate>> sendToKafka(ExchangeRate rate) {
        return Mono.fromFuture(
                kafkaTemplate.send(
                        exchangeRatesTopic,
                        rate.getCurrency(), // key
                        rate))
                .doOnSuccess(res -> {
                    var m = res.getRecordMetadata();
                    log.info("📈 Курс отправлен в Kafka: {} -> buy={}, sell={}, topic={}, partition={}, offset={}",
                            rate.getCurrency(), rate.getBuy(), rate.getSell(),
                            m.topic(), m.partition(), m.offset());
                })
                .doOnError(e -> log.error("Ошибка при отправке курса в Kafka", e));
    }
}