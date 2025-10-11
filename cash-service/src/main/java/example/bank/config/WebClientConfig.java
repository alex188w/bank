package example.bank.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // клиент с lb:// для микросервисов
    @Bean
    @LoadBalanced
    public WebClient loadBalancedWebClient(WebClient.Builder builder) {
        return builder.build();
    }

    // обычный клиент для localhost
    @Bean
    public WebClient plainWebClient() {
        return WebClient.create();
    }
}
