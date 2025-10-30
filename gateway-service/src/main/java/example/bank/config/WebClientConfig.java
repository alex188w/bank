package example.bank.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

        @Value("${clients.account.base-url:http://localhost:8082}")
        private String accountBaseUrl;

        @Value("${clients.exchange.base-url:http://localhost:8084}")
        private String exchangeBaseUrl;

        @Value("${clients.cash.base-url:http://localhost:8083}")
        private String cashBaseUrl;

        @Value("${clients.transfer.base-url:http://localhost:8086}")
        private String transferBaseUrl;

        private WebClient buildWebClient(String baseUrl, String clientRegistrationId) {
                var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, authService);
                var oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
                oauth2.setDefaultClientRegistrationId(clientRegistrationId);

                return WebClient.builder()
                                .baseUrl(baseUrl)
                                .filter(oauth2)
                                .build();
        }

        @Bean
        public WebClient accountWebClient() {
                return buildWebClient(accountBaseUrl, "gateway-client");
        }

        @Bean
        public WebClient cashWebClient() {
                return buildWebClient(cashBaseUrl, "gateway-client");
        }

        @Bean
        public WebClient exchangeWebClient() {
                return buildWebClient(exchangeBaseUrl, "gateway-client");
        }

        @Bean
        public WebClient transferWebClient() {
                return buildWebClient(transferBaseUrl, "gateway-client");
        }

        @Bean
        @Qualifier("keycloakWebClient")
        public WebClient keycloakWebClient() {
                return WebClient.builder().build(); // можно настроить базовый URL, фильтры и т.д.
        }
}
