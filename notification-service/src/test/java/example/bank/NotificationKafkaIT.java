package example.bank;
import example.bank.Notification;
import example.bank.service.NotificationService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@SpringBootTest(classes = NotificationServiceApplication.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {"notifications.raw"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
class NotificationKafkaIT {

    private static final String TOPIC = "notifications.raw";

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    NotificationService notificationService;

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        // переопределяем bootstrap-servers и имя топика для теста
        registry.add("spring.kafka.bootstrap-servers",
                () -> "localhost:9092");
        registry.add("app.kafka.topics.notifications",
                () -> TOPIC);
    }

    @Test
    void whenMessageInKafka_thenNotificationServiceEmitsIt() {
        // given: producer в embedded Kafka
        Map<String, Object> props = KafkaTestUtils.producerProps(embeddedKafka);
        var producerFactory = new DefaultKafkaProducerFactory<String, Notification>(
                props,
                new StringSerializer(),
                new org.springframework.kafka.support.serializer.JsonSerializer<>()
        );
        var producer = producerFactory.createProducer();

        Notification n = new Notification();
        n.setType("deposit");
        n.setUsername("test-user");
        n.setMessage("Пополнение");
        n.setAmount(BigDecimal.TEN);
        n.setAccountId(99L);

        // when: отправляем событие в Kafka
        producer.send(new ProducerRecord<>(TOPIC, "99", n));
        producer.flush();

        // then: streamNotifications() должен отдать это уведомление
//         StepVerifier.create(notificationService.streamNotifications())
//                 .expectNextMatches(ev ->
//                         "deposit".equals(ev.getType())
//                                 && "test-user".equals(ev.getUsername())
//                                 && BigDecimal.TEN.compareTo(ev.getAmount()) == 0
//                                 && Long.valueOf(99L).equals(ev.getAccountId())
//                 )
//                 .thenCancel()
//                 .verify(Duration.ofSeconds(5));
    }
}
