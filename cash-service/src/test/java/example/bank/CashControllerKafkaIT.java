package example.bank;

import example.bank.controller.CashController;
import example.bank.Notification;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

// @SpringBootTest(classes = CashServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
// @EmbeddedKafka(partitions = 1, topics = { CashControllerKafkaIT.TOPIC }, brokerProperties = {
//         "listeners=PLAINTEXT://localhost:9092",
//         "port=9092"
// })
// @TestPropertySource(properties = {
//         // чтобы @Value("${app.kafka.topics.notifications}") подхватился
//         "app.kafka.topics.notifications=" + CashControllerKafkaIT.TOPIC,

//         // привязываем spring-kafka к EmbeddedKafka
//         "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
//         "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
//         "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
//         "spring.kafka.producer.properties.spring.json.trusted.packages=example.bank",

//         // если security в тестах мешает — можно отключить автоконфиг:
//         "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration,"
//                 +
//                 "org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration"
// })
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// class CashControllerKafkaIT {

//     static final String TOPIC = "notifications.raw";

//     @Autowired
//     CashController cashController;

//     @Autowired
//     EmbeddedKafkaBroker embeddedKafka;

//     // Мокаем внешний вызов в account-service
//     @MockBean
//     WebClient accountWebClient;

//     @Test
//     void deposit_shouldPublishNotificationToKafka() {
//         // 1. Мокаем цепочку WebClient, чтобы не ходить в account-service
//         mockAccountWebClientDeposit();

//         // 2. Готовим consumer для EmbeddedKafka
//         Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("cash-test-group", "true", embeddedKafka);
//         consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "example.bank");

//         var consumerFactory = new DefaultKafkaConsumerFactory<String, Notification>(
//                 consumerProps,
//                 new StringDeserializer(),
//                 new JsonDeserializer<>(Notification.class, false));
//         var consumer = consumerFactory.createConsumer();
//         embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);

//         // 3. Вызываем ПУБЛИЧНЫЙ метод контроллера (НЕ приватный sendNotification!)
//         Long accountId = 21L;
//         BigDecimal amount = BigDecimal.valueOf(55);

//         cashController.deposit(accountId, amount)
//                 .block(Duration.ofSeconds(5));

//         // 4. Читаем сообщение из Kafka и проверяем
//         ConsumerRecord<String, Notification> record = KafkaTestUtils.getSingleRecord(consumer, TOPIC,
//                 Duration.ofSeconds(10));

//         assertThat(record).isNotNull();
//         assertThat(record.key()).isEqualTo(String.valueOf(accountId));

//         Notification n = record.value();
//         assertThat(n).isNotNull();
//         assertThat(n.getType()).isEqualTo("deposit");
//         assertThat(n.getAccountId()).isEqualTo(accountId);
//         assertThat(n.getAmount()).isEqualByComparingTo(amount);
//         assertThat(n.getMessage())
//                 .contains("Пополнение счёта")
//                 .contains(String.valueOf(accountId));
//     }

//     private void mockAccountWebClientDeposit() {
//         // Мокаем цепочку
//         // accountWebClient.post().uri(...).retrieve().bodyToMono(Void.class)
//         WebClient.RequestBodyUriSpec bodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
//         WebClient.RequestHeadersSpec<?> headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
//         WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

//         Mockito.when(accountWebClient.post()).thenReturn(bodyUriSpec);
//         Mockito.when(bodyUriSpec.uri(anyString(), any(), any()))
//                 .thenReturn(bodyUriSpec);
//         Mockito.when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
//         Mockito.when(responseSpec.bodyToMono(Void.class))
//                 .thenReturn(Mono.empty()); // считаем, что операция прошла успешно
//     }
// }