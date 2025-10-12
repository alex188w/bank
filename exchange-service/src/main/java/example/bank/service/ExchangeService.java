package example.bank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;

import example.bank.model.ExchangeRate;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final WebClient webClient = WebClient.create();

    private final Map<String, ExchangeRate> rates = new ConcurrentHashMap<>();

    @Value("${exchange.generator-url}")
    private String generatorUrl;

    @PostConstruct
    public void init() {
        // при старте подтягиваем актуальные курсы
        fetchRatesFromGenerator().subscribe(
                newRates -> {
                    updateRates(newRates);
                    log.info("Инициализация курсов валют завершена");
                },
                err -> log.warn("Не удалось получить курсы при старте: {}", err.getMessage()));
    }

    public Mono<List<ExchangeRate>> fetchRatesFromGenerator() {
        return webClient.get()
                .uri(generatorUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ExchangeRate>>() {
                });
    }

    public List<ExchangeRate> getAllRates() {
        return new ArrayList<>(rates.values());
    }

    public void updateRates(List<ExchangeRate> newRates) {
        newRates.forEach(rate -> rates.put(rate.getCurrency().toUpperCase(), rate));
    }

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        if (from.equalsIgnoreCase(to))
            return amount;

        ExchangeRate fromRate = rates.getOrDefault(from.toUpperCase(),
                new ExchangeRate(from, BigDecimal.ONE, BigDecimal.ONE));
        ExchangeRate toRate = rates.getOrDefault(to.toUpperCase(),
                new ExchangeRate(to, BigDecimal.ONE, BigDecimal.ONE));

        BigDecimal rubAmount = amount.multiply(fromRate.getSell());
        return rubAmount.divide(toRate.getBuy(), 2, RoundingMode.HALF_UP);
    }
}
