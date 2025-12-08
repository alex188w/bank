package example.bank;

import example.bank.CashServiceApplication;
import example.bank.controller.CashController;
import example.bank.Notification;
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

@SpringBootTest(classes = CashServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = { CashControllerKafkaIT.TOPIC }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashControllerKafkaIT {

    static final String TOPIC = "notifications.raw";

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    CashController cashController;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // embedded Kafka
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");

        // наш топик
        registry.add("app.kafka.topics.notifications", () -> TOPIC);

        // правильные сериализаторы для KafkaTemplate
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.trusted.packages",
                () -> "example.bank");
    }

    @Test
    void sendNotification_shouldPublishMessageToKafka() throws Exception {
        // --- готовим consumer ---
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("cash-test-group", "true", embeddedKafka);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "example.bank");

        var consumerFactory = new DefaultKafkaConsumerFactory<String, Notification>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(Notification.class, false));
        var consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);

        // --- вызываем приватный метод sendNotification через reflection ---
        Method m = CashController.class.getDeclaredMethod(
                "sendNotification",
                Long.class,
                String.class,
                BigDecimal.class,
                String.class);
        m.setAccessible(true);

        Long accountId = 123L;
        BigDecimal amount = BigDecimal.valueOf(50);

        @SuppressWarnings("unchecked")
        Mono<Void> mono = (Mono<Void>) m.invoke(
                cashController,
                accountId,
                "deposit",
                amount,
                null);

        // ждём, пока отправка в Kafka завершится
        mono.block(Duration.ofSeconds(10));

        // --- проверяем, что сообщение попало в topic ---
        ConsumerRecord<String, Notification> record = KafkaTestUtils.getSingleRecord(consumer, TOPIC,
                Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(String.valueOf(accountId));

        Notification n = record.value();
        assertThat(n).isNotNull();
        assertThat(n.getType()).isEqualTo("deposit");
        assertThat(n.getAccountId()).isEqualTo(accountId);
        assertThat(n.getAmount()).isEqualByComparingTo(amount);
        assertThat(n.getMessage())
                .contains("Пополнение счёта №" + accountId);
    }
}
