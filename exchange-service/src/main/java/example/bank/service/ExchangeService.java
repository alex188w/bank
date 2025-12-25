package example.bank.service;

import example.bank.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final Map<String, ExchangeRate> rates = new ConcurrentHashMap<>();
    private final ExchangeAudit audit;

    @KafkaListener(topics = "${app.kafka.topics.exchange-rates}", groupId = "exchange-service", containerFactory = "exchangeRateKafkaListenerContainerFactory")
    public void onRateUpdate(ExchangeRate rate) {
        if (rate == null || rate.getCurrency() == null || rate.getCurrency().isBlank()) {
            audit.warn("exchange.rate.update", "Invalid exchange rate received from Kafka");
            return;
        }

        String key = rate.getCurrency().toUpperCase(Locale.ROOT);
        rates.put(key, rate);

        audit.info("exchange.rate.update",
                String.format("Rate updated from Kafka: %s -> buy=%s, sell=%s",
                        key, rate.getBuy(), rate.getSell()));
    }

    public List<ExchangeRate> getAllRates() {
        List<ExchangeRate> list = new ArrayList<>(rates.values());
        audit.info("exchange.rates.list", "Rates list requested, size=" + list.size());
        return list;
    }

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        // amount НЕ логируем 
        if (from == null || to == null || amount == null) {
            audit.warn("exchange.convert", "Convert request invalid (null fields)");
            throw new IllegalArgumentException("Invalid request");
        }

        if (from.equalsIgnoreCase(to)) {
            audit.info("exchange.convert", "Convert requested: from=" + from + " to=" + to + " (same currency)");
            return amount;
        }

        ExchangeRate fromRate = rates.getOrDefault(from.toUpperCase(Locale.ROOT),
                new ExchangeRate(from, BigDecimal.ONE, BigDecimal.ONE));
        ExchangeRate toRate = rates.getOrDefault(to.toUpperCase(Locale.ROOT),
                new ExchangeRate(to, BigDecimal.ONE, BigDecimal.ONE));

        BigDecimal rubAmount = amount.multiply(fromRate.getSell());
        BigDecimal result = rubAmount.divide(toRate.getBuy(), 2, RoundingMode.HALF_UP);

        audit.info("exchange.convert", "Convert done: from=" + from + " to=" + to);
        return result;
    }
}
