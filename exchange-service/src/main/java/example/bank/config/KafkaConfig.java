package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaConfig {
    
    /**
     * Конфигурация фабрики слушателей Kafka.
     * Ключевой метод для включения трассировки (observation) в консьюмере.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        // ВКЛЮЧАЕМ НАБЛЮДЕНИЕ (OBSERVATION) ДЛЯ ТРАССИРОВКИ
        // Это автоматически добавит заголовки трассировки и создаст spans в Zipkin
        factory.getContainerProperties().setObservationEnabled(true);
        
        return factory;
    }
}
