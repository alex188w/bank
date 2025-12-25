package example.bank;

import example.bank.controller.TransferController;
import example.bank.dto.TransferRequest;
import example.bank.Notification;
import example.bank.TransferServiceApplication;
import example.bank.service.TransferService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(classes = TransferServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(partitions = 1, topics = { TransferControllerKafkaIT.TOPIC }, brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
})
@TestPropertySource(properties = {
                // чтобы @Value("${app.kafka.topics.notifications}") подхватился
                "app.kafka.topics.notifications=" + TransferControllerKafkaIT.TOPIC,

                // привязываем spring-kafka к EmbeddedKafka
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
                "spring.kafka.producer.properties.spring.json.trusted.packages=example.bank",

                // на случай, если security мешает в тестах
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration,"
                                +
                                "org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransferControllerKafkaIT {

        static final String TOPIC = "notifications.raw";

        @Autowired
        TransferController transferController;

        @Autowired
        EmbeddedKafkaBroker embeddedKafka;

        // Мокаем TransferService, чтобы не дергать реальные переводы и account-service
        @MockBean
        TransferService transferService;

        @Test
        void sendNotification_shouldPublishTransferNotificationToKafka() {
                // 1. Заглушаем бизнес-логику перевода: считаем, что transfer() всегда успешно
                // завершился
                Mockito.when(transferService.transfer(
                                anyString(), // fromUsername
                                anyLong(), // fromId
                                anyString(), // toUsername
                                anyLong(), // toId
                                any(BigDecimal.class) // amount
                ))
                                .thenReturn(Mono.empty());

                // 2. Готовим consumer для чтения из EmbeddedKafka
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("transfer-test-group", "true",
                                embeddedKafka);
                consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "example.bank");

                var consumerFactory = new DefaultKafkaConsumerFactory<String, Notification>(
                                consumerProps,
                                new StringDeserializer(),
                                new JsonDeserializer<>(Notification.class, false));
                var consumer = consumerFactory.createConsumer();
                embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);

                // 3. Вызываем ПУБЛИЧНЫЙ метод контроллера (НЕ приватный sendNotification!)
                TransferRequest req = new TransferRequest();
                req.setFromId(1L);
                req.setToId(2L);
                req.setFromUsername("from-user");
                req.setToUsername("to-user");
                req.setAmount(BigDecimal.valueOf(150));

        }
}