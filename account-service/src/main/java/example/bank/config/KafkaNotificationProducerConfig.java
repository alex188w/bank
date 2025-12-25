package example.bank.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.clients.producer.ProducerConfig;

import example.bank.Notification;

@Configuration
public class KafkaNotificationProducerConfig {

  @Bean(name = "notificationProducerFactory")
  public ProducerFactory<String, Notification> notificationProducerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
    props.put(org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
    props.put(ProducerConfig.ACKS_CONFIG, "1");
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean(name = "notificationKafkaTemplate")
  public KafkaTemplate<String, Notification> notificationKafkaTemplate(
      @Qualifier("notificationProducerFactory") ProducerFactory<String, Notification> pf) {
    return new KafkaTemplate<>(pf);
  }
}
