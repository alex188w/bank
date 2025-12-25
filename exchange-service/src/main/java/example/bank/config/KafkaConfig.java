package example.bank.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import example.bank.ExchangeRate;

@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, ExchangeRate> exchangeRateConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "exchange-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        //если несколько инстансов — можно оставить
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        JsonDeserializer<ExchangeRate> valueDeserializer = new JsonDeserializer<>(ExchangeRate.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer);
    }

    @Bean(name = "exchangeRateKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, ExchangeRate> exchangeRateKafkaListenerContainerFactory(
            ConsumerFactory<String, ExchangeRate> exchangeRateConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ExchangeRate> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(exchangeRateConsumerFactory);

        // создаёт spans для consume, и прокидывает tracing через headers
        factory.getContainerProperties().setObservationEnabled(true);

        return factory;
    }
}
