package example.bank;

import example.bank.TransferServiceApplication;
import example.bank.controller.TransferController;
import example.bank.Notification;
import example.bank.dto.TransferRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TransferServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = { TransferControllerKafkaIT.TOPIC }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransferControllerKafkaIT {

    static final String TOPIC = "notifications.raw";

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    TransferController transferController;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // Embedded Kafka
        registry.add("spring.kafka.bootstrap-servers",
                () -> "localhost:9092");

        // наш тестовый топик
        registry.add("app.kafka.topics.notifications",
                () -> TOPIC); // "notifications.raw"

        // конфиг продюсера, который использует kafkaTemplate
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.trusted.packages",
                () -> "example.bank");
    }

    @Test
    void sendNotification_shouldPublishTransferNotificationToKafka() throws Exception {
        // --- consumer ---
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("transfer-test-group", "true", embeddedKafka);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "example.bank");

        var consumerFactory = new DefaultKafkaConsumerFactory<String, Notification>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(Notification.class, false));
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);

        // --- private sendNotification(TransferRequest) через reflection ---
        Method m = TransferController.class.getDeclaredMethod(
                "sendNotification",
                TransferRequest.class);
        m.setAccessible(true);

        TransferRequest req = new TransferRequest();
        req.setFromId(1L);
        req.setToId(2L);
        req.setFromUsername("from-user");
        req.setToUsername("to-user");
        req.setAmount(BigDecimal.valueOf(150));

        @SuppressWarnings("unchecked")
        Mono<Void> mono = (Mono<Void>) m.invoke(transferController, req);

        mono.block(Duration.ofSeconds(10));

        // --- проверяем топик Kafka ---
        ConsumerRecord<String, Notification> record = KafkaTestUtils.getSingleRecord(consumer, TOPIC,
                Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(req.getToId()));

        Notification n = record.value();
        assertThat(n).isNotNull();
        assertThat(n.getType()).isEqualTo("transfer");
        assertThat(n.getFromId()).isEqualTo(req.getFromId());
        assertThat(n.getToId()).isEqualTo(req.getToId());
        assertThat(n.getAmount()).isEqualByComparingTo(req.getAmount());
        assertThat(n.getMessage())
                .contains("Перевод")
                .contains(String.valueOf(req.getFromId()))
                .contains(String.valueOf(req.getToId()));
    }
}
