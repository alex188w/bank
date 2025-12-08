package example.bank;

import example.bank.service.ExchangeService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExchangeServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = { "exchange.rates" }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
class ExchangeRatesKafkaIT {

    private static final String TOPIC = "exchange.rates";

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    ExchangeService exchangeService;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // куда подключается listener
        registry.add("spring.kafka.bootstrap-servers",
                () -> "localhost:9092");

        // имя топика для этого сервиса
        registry.add("app.kafka.topics.exchange-rates",
                () -> TOPIC); // "exchange.rates"

        // конфиг consumer'а
        registry.add("spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages",
                () -> "example.bank");

    }

    @Test
    void whenRatesArriveInKafka_thenServiceUpdatesInMemoryRates() throws Exception {
        // given: producer в embedded Kafka
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);

        var producerFactory = new DefaultKafkaProducerFactory<String, ExchangeRate>(
                producerProps,
                new StringSerializer(),
                new JsonSerializer<>());
        var producer = producerFactory.createProducer();

        ExchangeRate eur = new ExchangeRate(
                "EUR",
                BigDecimal.valueOf(89.65),
                BigDecimal.valueOf(90.54));

        // отправляем ОДИН ExchangeRate, как в реальной системе
        producer.send(new ProducerRecord<>(TOPIC, "EUR", eur));
        producer.flush();

        // ждём, пока listener отработает
        Thread.sleep(1000);

        // then: проверяем состояние сервиса
        var all = exchangeService.getAllRates();

        assertThat(all).anySatisfy(rate -> {
            assertThat(rate.getCurrency()).isEqualTo("EUR");
            assertThat(rate.getBuy()).isEqualByComparingTo("89.65");
            assertThat(rate.getSell()).isEqualByComparingTo("90.54");
        });
    }
}