package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient accountWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://bank-platform-account-service:8082") // account-service
                .build();
    }
}