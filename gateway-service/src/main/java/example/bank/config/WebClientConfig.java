package example.bank.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
// import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

        private final ReactiveClientRegistrationRepository clients;
        private final ReactiveOAuth2AuthorizedClientService authService;

        @Value("${clients.account.base-url:http://bank-platform-account-service:8082}")
        private String accountBaseUrl;

        @Value("${clients.exchange.base-url:http://bank-platform-exchange-service:8084}")
        private String exchangeBaseUrl;

        @Value("${clients.cash.base-url:http://bank-platform-cash-service:8083}")
        private String cashBaseUrl;

        @Value("${clients.transfer.base-url:http://bank-platform-transfer-service:8086}")
        private String transferBaseUrl;

        @Value("${clients.notification.base-url:http://bank-platform-notification-service:8087}")
        private String notificationBaseUrl;

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
        @Primary // теперь действительно работает
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
        public WebClient notificationWebClient() {
                return WebClient.builder()
                                .baseUrl("http://bank-platform-notification-service:8087")
                                .build();
        }

        @Bean
        public WebClient keycloakWebClient() {
                return WebClient.builder().build();
        }
}
