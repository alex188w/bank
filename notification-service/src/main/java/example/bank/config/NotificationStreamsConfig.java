package example.bank.config;

import example.bank.Notification;
import example.bank.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
public class NotificationStreamsConfig {

    private final NotificationService notificationService;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    @Bean
    public JsonSerde<Notification> notificationSerde() {
        JsonSerde<Notification> serde = new JsonSerde<>(Notification.class);
        serde.deserializer().addTrustedPackages("example.bank"); 
        return serde;
    }

    @Bean
    public KStream<String, Notification> notificationsStream(
            StreamsBuilder builder,
            JsonSerde<Notification> notificationSerde) {
        KStream<String, Notification> stream = builder.stream(
                notificationsTopic,
                Consumed.with(Serdes.String(), notificationSerde));

        stream
                .filter((key, value) -> value != null)
                .peek((key, value) -> notificationService.publish(value));

        return stream;
    }
}