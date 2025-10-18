package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

        private final ReactiveClientRegistrationRepository clients;
        private final ReactiveOAuth2AuthorizedClientService authService;

        private WebClient buildWebClient(String baseUrl, String clientRegistrationId) {
                var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, authService);
                var oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
                oauth2.setDefaultClientRegistrationId(clientRegistrationId);

                return WebClient.builder()
                                .baseUrl(baseUrl)
                                .filter(oauth2) // OAuth2 фильтр
                                // Трассировка Sleuth автоматически добавится, ручной filter не нужен
                                .build();
        }

        private ExchangeFilterFunction logRequest() {
                return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                        System.out.println("Request______________: " + clientRequest.url());
                        clientRequest.headers().forEach(
                                        (name, values) -> values.forEach(v -> System.out.println(name + ": " + v)));
                        return Mono.just(clientRequest);
                });
        }

        @Bean
        public WebClient accountWebClient() {
                return buildWebClient("http://localhost:8082", "gateway-client");
        }

        @Bean
        public WebClient cashWebClient() {
                return buildWebClient("http://localhost:8083", "gateway-client");
        }

        @Bean
        public WebClient exchangeWebClient() {
                return buildWebClient("http://localhost:8084", "gateway-client");
        }
}
