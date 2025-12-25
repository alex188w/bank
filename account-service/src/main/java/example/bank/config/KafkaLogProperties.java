package example.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logs.kafka")
public record KafkaLogProperties(
        boolean enabled,
        String topic,
        String env,
        String service
) {}
