package example.bank;

import example.bank.service.RealRateUpdater;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExchangeGeneratorApplication.class)
@EmbeddedKafka(partitions = 1, topics = { RealRateUpdaterKafkaIT.TOPIC }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
class RealRateUpdaterKafkaIT {

    static final String TOPIC = "exchange.rates";

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    RealRateUpdater realRateUpdater;

    static MockWebServer mockWebServer;

    @BeforeAll
    static void startServer() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void shutDown() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> "localhost:9092");
        registry.add("app.kafka.topics.exchange-rates",
                () -> TOPIC);

        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.trusted.packages",
                () -> "example.bank");

        registry.add("exchange.api-url",
                () -> mockWebServer.url("/rates").toString());
    }

    @Test
    void whenUpdateRatesFromApi_thenMessageIsSentToKafka() throws Exception {
        // given: мокаем внешний API
        String json = """
                {
                  "base_code": "RUB",
                  "rates": {
                    "RUB": 1.0,
                    "USD": 0.011,
                    "EUR": 0.010
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        // consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("exchange-gen-test", "true", embeddedKafka);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "example.bank");

        var consumerFactory = new DefaultKafkaConsumerFactory<String, Object>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(Object.class, false));
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);

        // when: явный вызов (плюс, возможно, @PostConstruct уже дернул)
        realRateUpdater.updateRatesFromApi();
        Thread.sleep(2000); // даём время на отправку

        // then: проверяем, что в топике есть хотя бы одно сообщение
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count())
                .as("Должно быть хотя бы одно сообщение с курсами")
                .isGreaterThanOrEqualTo(1);

        var record = records.iterator().next();
        assertThat(record).isNotNull();
        assertThat(record.value()).isNotNull();

        System.out.println("Kafka value = " + record.value());
    }
}
