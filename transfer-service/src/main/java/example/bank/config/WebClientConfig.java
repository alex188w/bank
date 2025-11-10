package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {


    @Bean
    public WebClient notificationWebClient() {
        // без OAuth2, просто прямое соединение
        return WebClient.builder()
                .baseUrl("http://bank-platform-notification-service:8087") // notification-service
                .build();
    }
}