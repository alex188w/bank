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

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final Map<String, ExchangeRate> rates = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${app.kafka.topics.exchange-rates}",
            groupId = "exchange-service"
    )
    public void onRateUpdate(ExchangeRate rate) {
        if (rate == null || rate.getCurrency() == null) {
            log.warn("Получен некорректный курс: {}", rate);
            return;
        }
        String key = rate.getCurrency().toUpperCase();
        rates.put(key, rate);
        log.info("💰 Обновлён курс из Kafka: {} -> buy={}, sell={}", key, rate.getBuy(), rate.getSell());
    }

    public List<ExchangeRate> getAllRates() {
        List<ExchangeRate> list = new ArrayList<>(rates.values());
        log.info("📊 Текущее количество курсов в памяти: {}", list.size());
        return list;
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
