package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient accountWebClient(ReactiveClientRegistrationRepository clients,
            ReactiveOAuth2AuthorizedClientService authService) {

        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, authService);
        var oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("cash-service-client");

        return WebClient.builder()
                .baseUrl("http://bank-platform-account-service:8082")
                .filter(oauth2)
                .build();
    }

    // @Bean
    // public WebClient accountWebClient() {
    //     return WebClient.builder()
    //             .baseUrl("http://bank-platform-account-service:8082") // для настройки без OAuth2AuthorizedClientManager account-service
    //             .build();
    // }

    // @Bean
    // public WebClient notificationWebClient() {
    // // без OAuth2, просто прямое соединение
    // return WebClient.builder()
    // .baseUrl("http://bank-platform-notification-service:8087") //
    // notification-service
    // .build();
    // }
}